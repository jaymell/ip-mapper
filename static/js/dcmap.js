d3.queue()
  .defer(getLogs, "/json")
  .defer(d3.json, "/geojson/countries.geojson")
  .await(makeGraphs);


function exists(obj, key) {
  return typeof obj[key] !== 'undefined';
}

// once logging json is received, iterate through IPs and 
// concurrently hit API for geolocation data for each of them
//var ipLocateRoute = "/iplocate";
var ipLocateRoute = "/mock";
function geolocate(err, json, callback) {
  if (err) callback(err);
  json.forEach(function(item, index) {
    d3.json(ipLocateRoute + "?ip=" + item.ip, function(err, location) {
      if (err) callback(err);
      location = location.Result;
      Object.keys(location).forEach(function(v) {
        item[v] = location[v];
      });
    });
  });
}

// get first ip from comma-sep'd list of ips
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

var ipdim = [];

function makeGraphs(error, json, worldJson) {
  var worldChart = dc.geoChoroplethChart("#world-chart");

  // json.forEach(function(i) {
  //   console.log(i)
  // });
  // var timeChart = 

  // crossfilter
  var ndx = crossfilter(json);
  ipdim = json;
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

  console.log("here");  

  var countryDim = ndx.dimension(function(d) {
    if (exists(d,"country_iso")) {
      return d["country_iso"];
    }
  });


  // metrics
  var totalIpsByCountry = countryDim.group().reduceCount(function(d) {
    console.log(d);
    if (exists(d, "country_iso")) {
      return d["country_iso"];
    }
  });

  var maxCountry = totalIpsByCountry.top(1)[0].value;

  var height = 800;
  var width = 1600;

  var projection = d3.geo.equirectangular()
                     .scale(200)
                     .center([-97,33]);

  function zoomed() {
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

  var svg = d3.select("#world-chart")
              .attr("width", width)
              .attr("height", height)
              .call(zoom);


/*
    timeChart
        .width(600)
        .height(160)
        .margins({top: 10, right: 50, bottom: 30, left: 50})
        .dimension(dateDim)
        .group(numProjectsByDate)
        .transitionDuration(500)
        .x(d3.time.scale().domain([minDate, maxDate]))
        .elasticY(true)
        .xAxisLabel("Year")
        .yAxis().ticks(4);
*/
    worldChart
        .height(height)
        .width(width)
        .dimension(ipDim)
        .group(totalIpsByCountry)
        .colors(["#E2F2FF", "#C4E4FF", "#9ED2FF", "#81C5FF", "#6BBAFF", "#51AEFF", "#36A2FF", "#1E96FF", "#0089FF", "#0061B5"])
        //.colorDomain([0, maxCountry])
        .colorDomain([0, 10])
        .colorCalculator(function (d) { return d ? worldChart.colors()(d) : '#ccc';})
        .overlayGeoJson(worldJson["features"], "country", function (d) {
            return d.properties.iso_a3;
        })
        .projection(projection)
        .title(function (d) {
            var country = d.key;
            var total = d.value ? d.value : 0;
            return "Country: " + country
                 + "\n"
                 + "Total Sites: " + total + " Sites";
        })

  dc.renderAll();

}
