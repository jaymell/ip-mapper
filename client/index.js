const Promise = require('bluebird');
const d3 = require('d3');
const geo = require('d3-geo');
const scale = require('d3-scale');
const dc = require('dc');
const queue = require('d3-queue');
const time = require('d3-time');
const crossfilter = require('crossfilter');
const http = require('http');
const jsonlines = require('jsonlines');
const ipaddr = require('ipaddr.js');
const _ = require('lodash');
const geoJson = require('./countries.geojson.json');
const timer = require('./timer.js');

const jsTimer = timer('javscript');

function exists(obj, key) {
  return ( ! _.isNil(obj[key]) )
}

function existsArr(obj, keys) {
  return _.chain(keys)
    .map(it => exists(obj, it) ? it : undefined)
    .filter(it => !_.isUndefined(it))
    .value()
}

function getLogs(path) {
  return new Promise((res, rej) => {
    const parser = jsonlines.parse();

    http.get(path, resp => {
      resp.pipe(parser);
    });

    const arr = [];

    parser.on('data', d => {
      arr.push(d)
    });

    parser.on('end', () => {
      console.log(`length of json: ${arr.length}`)
      return res(arr);
    });

    parser.on('error', e => {
      return rej(e);
    });
  });
}

// clean up some of the fields for easier handling later
function cleanup(json) {
  const keys = ["x-forwarded-for", "remoteAddress"];
  return _.chain(json)
    .map(it => {
      _.forEach(keys, k => {
        if ( exists(it, k) ) {
          it[k] = cleanIp(it[k]);
          if ( ( ! ipaddr.isValid(it[k]) ) || ipaddr.parse(it[k]).range() !== 'unicast' ) {
            delete it[k]
          } else {
            it[k] = it[k].toString();
          }
        }
      });
      if (exists(it, 'date')) {
        it.date = new Date(it.date.$date);
      }
      delete it._id;
      return it;
    })
    // convert to pair, 1st element is containing keys (maybe empty), 2nd is it:
    .map(it => [ existsArr(it, keys), it ] )
    // remove empties:
    .filter(pair => ( ! _.isEmpty(pair[0])) )
    // cleanIp on each found ip field and set first to 'ip' key and clean up other fields:
    .map(pair => {
      let [keys, it] = pair;
      it.ip  = it[keys[0]];
      return it;
    })
    .value();
}

// get first ip from comma-sep'd list of ips;
// may want these later but for now just complicate things
function cleanIp(ip) {
  const idxComma = ip.indexOf(",");
  const singleIp = idxComma > -1 ? ip.slice(0, idxComma) : ip;
  const idxColon = singleIp.lastIndexOf(":");
  const cleanedIp = idxColon > -1 ? singleIp.slice(idxColon+1) : singleIp
  return cleanedIp;
}

function geolocate(ip) {
  const ipLocateRoute = "/geolocate";
  return new Promise((res, rej) => {
    http.get(ipLocateRoute + "?ip=" + ip, resp => {
      var body = '';
      resp.on('data', d => body += d);
      resp.on('error', e => rej(e));
      resp.on('end', () => res(JSON.parse(body)));
    })
  })
}

// build the charts
function makeCharts(json, worldJson) {

  var charts = {}; // this gets returned

  console.log('got this json: ', json);
  charts.ndx = crossfilter(json);

  charts.allDim = charts.ndx.dimension(d => d);
  charts.ipDim = charts.ndx.dimension(d => exists(d, "ip") ? d["ip"] : undefined);
  charts.dateDim = charts.ndx.dimension(d => exists(d, "date") ? d['date'] : undefined);

  charts.getMinDate = function() { return charts.dateDim.bottom(1)[0]["date"]; }
  charts.getMaxDate = function() { return charts.dateDim.top(1)[0]["date"]; }

  charts.countryCodeDim = charts.ndx.dimension(d => exists(d,"country_iso") ? d["country_iso"] : undefined);
  charts.hitsByCountryCode = charts.countryCodeDim.group();

  charts.countryNameDim = charts.ndx.dimension(d => exists(d,"country") ? d["country"] : undefined);
  charts.hitsByCountryName = charts.countryNameDim.group();
  charts.hitsByDate = charts.dateDim.group(d => d3.timeHour(d));

  charts.urlDim = charts.ndx.dimension(d => exists(d, "url") ? d["url"] : undefined);
  charts.hostDim = charts.ndx.dimension(d => exists(d, "host") ? d["host"] : undefined);
  charts.useragentDim = charts.ndx.dimension(d => exists(d, "user-agent") ? d["user-agent"] : undefined);

  // chart objects
  var worldChartDiv = "#world-chart";
  var totalHitsDiv = "#total-hits";
  var timeChartDiv = '#time-chart';
  var pieChartDiv = '#pie-chart';
  var urlTableDiv = '#url-table';
  var hostTableDiv = '#host-table';
  var useragentDiv = '#useragent-table';

  charts.worldChart = dc.geoChoroplethChart(worldChartDiv);
  charts.totalHits = dc.numberDisplay(totalHitsDiv);
  charts.timeChart = dc.lineChart(timeChartDiv);
  charts.pieChart = dc.pieChart(pieChartDiv);
  charts.urlTable = dc.dataTable(urlTableDiv);
  charts.hostTable = dc.dataTable(hostTableDiv);
  charts.useragentTable = dc.dataTable(useragentDiv);
  var projection = geo.geoEquirectangular()
                     // .scale(50)
                     .center([0,0]);

/*
  var zoomed = function() {
    projection
      .translate(d3.event.translate)
      .scale(d3.event.scale);
    worldChart.render();
  }

  var zoom = d3.behavior.zoom()
               .translate(projection.translate())
               //.scale(projection.scale())
               .scale(1 << 8) // not sure why, but 8 seems like a good number
               //.scaleExtent([height/2, 8 * height]) // defines the max amount you can zoom
               .on("zoom", zoomed);

  d3.select("#world-chart")
   .call(zoom);
*/

  charts.worldChart
    .height(500)
    .width(1000)
    .dimension(charts.countryCodeDim)
    .group(charts.hitsByCountryCode)
    .transitionDuration(500)
    .colors(scale.scaleQuantize().range(["#ffe6e6", "#ffcccc", "#ffb3b3",
                                       "#ff9999", "#ff8080", "#ff6666",
                                       "#ff4d4d", "#ff3333", "#ff1a1a",
                                       "#ff0000"]))
    .colorDomain([0, 10])
    .colorCalculator(function (d) {
      return d ? charts.worldChart.colors()(d) : '#99ff99';
    })
    .overlayGeoJson(worldJson["features"], "country", function (d) {
        return d.properties.iso_a2;
    })
    .projection(projection)
    .title(function (d) {
        var country = d.key;
        var total = d.value ? d.value : 0;
        return "Country: " + country
             + "\n"
             + "Total Hits: " + total + " Hits";
    });


  charts.totalHits
    .group(charts.allDim.group())
    .formatNumber(d3.format('g'))
    .value(function(d) { return d; });

  // use this if want to potentially remove 0
  // values to allow x-axis resizing (though it skews data,
  // b/c it will always look like at least one hit)
  function remove_empty_bins(source_group) {
      function non_zero_pred(d) {
          return d.value != 0;
      }
      return {
          all: function () {
              return source_group.all().filter(non_zero_pred);
          },
          top: function(n) {
              return source_group.top(Infinity)
                  .filter(non_zero_pred)
                  .slice(0, n);
          }
      };
  }
  // charts.hitsByDate = remove_empty_bins(charts.hitsByDate);

  charts.timeChart
    .width(800)
    .height(160)
    .margins({top: 10, right: 50, bottom: 30, left: 50})
    .dimension(charts.dateDim)
    .group(charts.hitsByDate)
    .transitionDuration(100)
    .elasticX(true)
    .elasticY(true)
    .x(d3.scaleTime().domain([charts.getMinDate(), charts.getMaxDate()]))
    // .gap(1);

  charts.timeChart.yAxis().ticks(5);
  charts.timeChart.xUnits(d3.timeDays);  // this prevents skinny "1-second" bars

  charts.pieChart
    .height(300)
    .width(300)
    .dimension(charts.countryNameDim)
    .group(charts.hitsByCountryName);


  charts.urlTable
    .dimension(remove_empty_bins(charts.urlDim.group()))
    .group(function(d) {
      return "";
    })
    .columns([
      {
        label: "Count",
        format: function(d) { return d.value; }
      },
      {
        label: "URL",
        format: function(d) { return d.key; }
      }
    ])
    .size(100)
    .sortBy(function(d) { return d.value; })
    .order(d3.descending);


  charts.hostTable
    .dimension(remove_empty_bins(charts.hostDim.group()))
    .group(function(d) {
      return "";
    })
    .columns([
      {
        label: "Count",
        format: function(d) { return d.value; }
      },
      {
        label: "Host",
        format: function(d) { return d.key; }
      }
    ])
    .size(100)
    .sortBy(function(d) { return d.value; })
    .order(d3.descending);


  charts.useragentTable
    .dimension(remove_empty_bins(charts.useragentDim.group()))
    .group(function(d) {
      return "";
    })
    .columns([
      {
        label: "Count",
        format: function(d) { return d.value; }
      },
      {
        label: "User-agent String",
        format: function(d) { return d.key; }
      }

    ])
    .size(100)
    .sortBy(function(d) { return d.value; })
    .order(d3.descending);


  var done = function() {
    dc.renderAll();
    d3.selectAll('#loader')
      .transition()
      .duration(1000)
      .style("opacity", 0);
    d3.selectAll('.chart-container')
      .transition()
      .duration(1000)
      .style('opacity', 1);
  }();

  jsTimer.end();

  return charts;
}

async function main() {
  jsTimer.begin();
  const _d = new Date();
  const d = new Date().setMonth(_d.getMonth() - 3);
  const json = cleanup(await getLogs(`/json?gte=${d}`));
  // // global_json = json;
  const cache = {};
  const geolocated = await Promise.map(json, (async(it) => {
      try {
        if ( it.ip in cache ) {
          return _.assign(it, cache[it.ip]);
        }
        const geolocated = await geolocate(it.ip);
        cache[it.ip] = geolocated;
        return _.assign(it, geolocated);
      } catch (e) {
        console.error(`Failed geolocation for ${it.ip}`, e);
        return it;
      }
  }), { concurrency: 5 });
  delete cache;
  console.log('geolocated: ', geolocated);
  // global_geolocated = geolocated;
  console.log('geojson: ', geoJson);
  // delete json;
  makeCharts(geolocated, geoJson);
}

function reset() {
  dc.filterAll();
  dc.renderAll();
}

main();
window.reset = reset;

module.exports = {
    cleanup: cleanup,
    cleanIp: cleanIp,
    exists: exists,
    existsArr: existsArr,
    reset: reset
};
