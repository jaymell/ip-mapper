var jsTimer = timer('javscript');
jsTimer.begin();

// set by await function:
var charts;

d3.queue()
  .defer(getLogs, "/json")
  .defer(d3.json, "/geojson/countries.geojson")
  .await(function(error, json, worldJson) {
    charts = makeCharts(error, json, worldJson)
  });

function exists(obj, key) {
  return key in obj;
}

// get logging data from API
function getLogs(path, callback) {
  d3.json(path, parseJson(callback));
}


// clean up some of the fields for easier handling later
function parseJson(callback) {
  return function(err, json) {
    if (err) callback(err);
    if (json['Status'] !== 200) callback(null);

    var result = json.Result;
    var cleaned = [];
    
    for(var i=0; i<result.length; i++) {
      if ( exists(result[i], "x-forwarded-for") ) {
        var ip = cleanIp(result[i]["x-forwarded-for"]);
        delete result[i]["x-forwarded-for"];
      } 
      else if ( exists(result[i], "remoteAddress") ) {
        // remove extra formatting express puts in place:
        var ip = cleanIp(result[i]["remoteAddress"].replace(/^.*:/, ''));
        delete result[i]["remoteAddress"];
      } 
      else {
        console.log("no ip found for: ", result[i]);
        continue;
      }
      if (ipaddr.isValid(ip)) {
        ip = ipaddr.parse(ip);
        // skip private and localhost addresses:
        if (ip.range() !== 'unicast') {
          continue;
        }
      } else {
        continue;
      }
      result[i]["ip"] = ip.toString();
      result[i]["date"] = new Date(result[i]["date"]);
      delete result[i]["_id"];
      cleaned.push(result[i]);
    }

    geolocate(null, cleaned, callback);
  }
}


// get first ip from comma-sep'd list of ips;
// may want these later but for now just complicate things
function cleanIp(ip) {
  idx = ip.indexOf(",")
  if (idx > -1) {
    return ip.slice(0, idx);
  }
  return ip
}

// set up queue, iterate through json and 
// pass IPs off to lookupIp for further handling
function geolocate(err, json, callback) {

  var geoTimer = timer('geolocate');
  geoTimer.begin();
  if (err) callback(err);
  var locations = {}; // hold responses
  var q = d3.queue();
  
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
function makeCharts(error, json, worldJson) {
  console.log(json);

  var ndx = crossfilter(json);

  var allDim = ndx.dimension(function(d) {
    return d;
  })

  var ipDim = ndx.dimension(function(d) { 
    if (exists(d, "ip")) { 
      return d["ip"];
    }
  });

  var dateDim = ndx.dimension(function(d) {
    if (exists(d, "date")) {
      return d['date'];
    }
  });
  var minDate = dateDim.bottom(1)[0]["date"];
  var maxDate = dateDim.top(1)[0]["date"];

  var countryCodeDim = ndx.dimension(function(d) {
    if (exists(d,"country_iso")) {
      return d["country_iso"];
    }
  });

  var hitsByCountryCode = countryCodeDim.group();

  var countryNameDim = ndx.dimension(function(d) {
    if (exists(d,"country")) {
      return d["country"];
    }
  })
  var hitsByCountryName = countryNameDim.group();
    
  var hitsByDate = dateDim.group(function(d) {
    return d3.time.day(d);
  });

  var urlDim = ndx.dimension(function(d) {
    if (exists(d, "url")) {
      return d["url"];
    }
  });

  var hostDim = ndx.dimension(function(d) {
    if (exists(d, "host")) {
      return d["host"];
    }
  });

  var useragentDim = ndx.dimension(function(d) {
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

  var charts = {}; // this gets returned 
  charts.worldChart = dc.geoChoroplethChart(worldChartDiv);
  charts.totalHits = dc.numberDisplay(totalHitsDiv);
  charts.timeChart = dc.barChart(timeChartDiv);
  charts.pieChart = dc.pieChart(pieChartDiv);
  charts.urlTable = dc.dataTable(urlTableDiv);
  charts.hostTable = dc.dataTable(hostTableDiv);
  charts.useragentTable = dc.dataTable(useragentDiv);
  var projection = d3.geo.equirectangular()
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
    .dimension(countryCodeDim)
    .group(hitsByCountryCode)
    .transitionDuration(500)
    .colors(d3.scale.quantize().range(["#ffe6e6", "#ffcccc", "#ffb3b3", 
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
    .group(allDim.group())
    .formatNumber(d3.format('g'))
    .value(function(d) { return d; });

  charts.timeChart
    .width(800)
    .height(160)
    .margins({top: 10, right: 50, bottom: 30, left: 50})
    .dimension(dateDim)
    .group(hitsByDate)
    .transitionDuration(500)
    .x(d3.time.scale().domain([minDate, maxDate]))
    .elasticX(true)
    .elasticY(true)
    .gap(1);

  charts.timeChart.yAxis().ticks(5);
  charts.timeChart.xUnits(d3.time.days);  // this prevents skinny "1-second" bars

  charts.pieChart
    .height(300)
    .width(300)
    .dimension(countryNameDim)
    .group(hitsByCountryName);

  charts.urlTable
    .dimension(urlDim.group())
    .group(function(d) {
      return "";
    })
    .columns([
      {
        label: "URL",
        format: function(d) { return d.key; }
      },
      {
        label: "Count",
        format: function(d) { return d.value; }  
      }
    ])
    .size(100)
    .sortBy(function(d) { return d.value; })
    .order(d3.descending);

  charts.hostTable
    .dimension(hostDim.group())
    .group(function(d) {
      return "";
    })
    .columns([
      {
        label: "Host",
        format: function(d) { return d.key; }
      },
      {
        label: "Count",
        format: function(d) { return d.value; }  
      }
    ])
    .size(100)
    .sortBy(function(d) { return d.value; })
    .order(d3.descending);


  charts.useragentTable
    .dimension(useragentDim.group())
    .group(function(d) {
      return "";
    })
    .columns([
      {
        label: "User-agent String",
        format: function(d) { return d.key; }
      },
      {
        label: "Count",
        format: function(d) { return d.value; }
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
