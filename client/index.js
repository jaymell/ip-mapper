const Promise = require('bluebird');
const d3 = require('d3');
const geo = require('d3-geo');
const scale = require('d3-scale');
const dc = require('dc');
const queue = require('d3-queue');
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
    return _.chain(json)
        // convert to pair, 1st element is containing keys (maybe empty), 2nd is it:
        .map(it => [ existsArr(it, ["x-forwarded-for", "remoteAddress"]), it ] )
        // remove empties:
        .filter(pair => ! _.isEmpty(pair[0]))
        // cleanIp on each found ip field and clean up other fields:
        .map(pair => {
            let [ keys, it ] = pair;
            _.forEach(keys, k => {
                it[k] = cleanIp(it[k]);
                if ( ! ipaddr.isValid(it[k]) || ipaddr.parse(it[k]).range() !== 'unicast' ) {
                    delete it[k]
                } else {
                    it[k] = it[k].toString();
                }
            })

            it.date = new Date(it.date.$date);
            delete it._id;
            return it;
        })
        .value();
}

// get first ip from comma-sep'd list of ips;
// may want these later but for now just complicate things
function cleanIp(ip) {
  const idx = ip.indexOf(",")
  if (idx > -1) {
    return ip.slice(0, idx);
  }
  return ip
}

// set up queue, iterate through json and
// pass IPs off to lookupIp for further handling
// FIXME: convert this to promises:
function geolocate(err, json, callback) {

  var geoTimer = timer('geolocate');
  geoTimer.begin();
  if (err) callback(err);
  var locations = {}; // hold responses
  var q = queue.queue();

  var date = new Date();
  console.log("Start geolocating: ", date.toISOString());
  json.forEach(function(item) {
    var ip = item.ip;
    if (ip in locations) {
      console.log("duplicate -- skipping");
      return;
    }
      locations[ip] = null;
      q.defer(lookupIp, ip, locations);
  });

  q.awaitAll(function(err) {
    geoTimer.end(json.length);
    if (err) callback(err);
    for(var i=0; i<json.length; i++) {
      var ip = json[i].ip;
      if (exists(locations, ip) && locations.ip !== null) {
        Object.keys(locations[ip]).forEach(function(item) {
          json[i][item] = locations[ip][item];
        });
      }
    }
    var date = new Date();
    callback(null, json);
  });
}


// call api for lookups, avoiding duplicate calls
// FIXME: promisify this
function lookupIp(ip, locations, callback) {
  // var ipLocateRoute = "/mock";
  var ipLocateRoute = "/iplocate";
  d3.json(ipLocateRoute + "?ip=" + ip, function(err, resp) {
    if (err) return callback(null);
    locations[ip] = resp["Result"];
    callback(null);
  });
}

// build the charts
function makeCharts(json, worldJson) {

  var charts = {}; // this gets returned

  charts.ndx = crossfilter(json);

  charts.allDim = charts.ndx.dimension(function(d) {
    return d;
  })

  charts.ipDim = charts.ndx.dimension(function(d) {
    if (exists(d, "ip")) {
      return d["ip"];
    }
  });

  charts.dateDim = charts.ndx.dimension(function(d) {
    if (exists(d, "date")) {
      return d['date'];
    }
  });

  charts.getMinDate = function() { return charts.dateDim.bottom(1)[0]["date"]; }
  charts.getMaxDate = function() { return charts.dateDim.top(1)[0]["date"]; }

  charts.countryCodeDim = charts.ndx.dimension(function(d) {
    if (exists(d,"country_iso")) {
      return d["country_iso"];
    }
  });

  charts.hitsByCountryCode = charts.countryCodeDim.group();

  charts.countryNameDim = charts.ndx.dimension(function(d) {
    if (exists(d,"country")) {
      return d["country"];
    }
  })
  charts.hitsByCountryName = charts.countryNameDim.group();

  charts.hitsByDate = charts.dateDim.group(function(d) {
    return d3.time.hour(d);
  });

  charts.urlDim = charts.ndx.dimension(function(d) {
    if (exists(d, "url")) {
      return d["url"];
    }
  });

  charts.hostDim = charts.ndx.dimension(function(d) {
    if (exists(d, "host")) {
      return d["host"];
    }
  });

  charts.useragentDim = charts.ndx.dimension(function(d) {
    if(exists(d, "user-agent")) {
      return d["user-agent"];
    }
  });

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
    .x(d3.time.scale().domain([charts.getMinDate(), charts.getMaxDate()]))
    // .gap(1);

  charts.timeChart.yAxis().ticks(5);
  charts.timeChart.xUnits(d3.time.days);  // this prevents skinny "1-second" bars

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
  const logs = await getLogs("/json?gte=1525156758000");
  const json = cleanup(logs);
  //const geolocated = geolocate(cleaned, callback);
  //makeCharts(geolocated, geoJson);
  //makeCharts(json, geoJson);
}

main();

module.exports = {
    cleanup: cleanup
};