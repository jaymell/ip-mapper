var date = new Date();
console.log("Begin: ", date.toISOString());

d3.queue()
  .defer(getLogs, "/json")
  .defer(d3.json, "/geojson/countries.geojson")
  .await(makeGraphs);


function exists(obj, key) {
  return typeof obj[key] !== 'undefined';
}

// call api for lookups, avoiding duplicate calls
function lookupIp(ip, locations, callback) {
  // var ipLocateRoute = "/mock";
  var ipLocateRoute = "/iplocate";
  d3.json(ipLocateRoute + "?ip=" + ip, function(err, resp) {
    if (err) callback(err);
    locations[ip] = resp["Result"];
    callback(null);
  });
}

// set up queue, iterate through json and 
// pass IPs off to lookupIp for further handling
function geolocate(err, json, callback) {
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
    console.log('await function called');
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
    console.log("Done geolocating: ", date.toISOString());
    callback(null, json);
  });
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

// clean up some of the fields for easier handling later
function parseJson(callback) {
  return function(err, json) {
    if (err) callback(err);
    if (json['Status'] !== 200) callback(null);

    var result = json.Result;
    var cleaned = [];
    
    for(var i=0; i<result.length; i++) {
      if ( exists(result[i], "x-forwarded-for") ) {
        result[i]["ip"] = cleanIp(result[i]["x-forwarded-for"]);
        delete result[i]["x-forwarded-for"];
      } 
      else if ( exists(result[i], "remoteAddress") ) {
        // remove extra formatting express puts in place:
        result[i]["ip"] = cleanIp(result[i]["remoteAddress"].replace(/^.*:/, ''));
        delete result[i]["remoteAddress"];
      } 
      else {
        console.log("no ip found for: ", result[i]);
        continue;
      }
      result[i]["date"] = new Date(result[i]["date"]);
      delete result[i]["_id"];
      cleaned.push(result[i]);
    }
    geolocate(null, cleaned, callback);
  }
}


// get logging data from API
function getLogs(path, callback) {
  d3.json(path, parseJson(callback));
}


// build the charts
function makeGraphs(error, json, worldJson) {
  console.log(json);

  // crossfilter
  var ndx = crossfilter(json);

  //  dimensions
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

  var countryDim = ndx.dimension(function(d) {
    if (exists(d,"country_iso")) {
      return d["country_iso"];
    }
  });

  // metrics
  var hitsByCountry = countryDim.group();

  var hitsByDate = dateDim.group(function(d) {
    return d3.time.day(d);
  });

  var maxCountry = hitsByCountry.top(1)[0].value;

  // chart objects
  var worldChartDiv = "#world-chart";
  var timeChartDiv = '#time-chart';
  var worldChart = dc.geoChoroplethChart(worldChartDiv);
  var timeChart = dc.barChart(timeChartDiv);
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

  timeChart
        .width(800)
        .height(160)
        .margins({top: 10, right: 50, bottom: 30, left: 50})
        .dimension(dateDim)
        .group(hitsByDate)
        .transitionDuration(500)
        .x(d3.time.scale().domain([minDate, maxDate]))
        .elasticY(true)
        .gap(1);
  
  timeChart.yAxis().ticks(5);
  timeChart.xUnits(d3.time.days);  // this prevents skinny "1-second" bars

  worldChart
        .height(500)
        .width(1000)
        .dimension(countryDim)
        .group(hitsByCountry)
        .transitionDuration(500)
        .colors(d3.scale.quantize().range(["#ffe6e6", "#ffcccc", "#ffb3b3", "#ff9999", "#ff8080", "#ff6666", "#ff4d4d", "#ff3333", "#ff1a1a", "#ff0000"]))
        .colorDomain([0, 10])
        .colorCalculator(function (d) { return d ? worldChart.colors()(d) : '#99ff99';})
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
        })
  dc.renderAll();

}
