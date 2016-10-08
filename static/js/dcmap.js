var q = d3.queue();
  q.defer(d3.json, "/json")
  q.defer(d3.json, "/geojson/countries.geojson")
  q.await(makeGraphs);

function makeGraphs(error, sitesJson, worldJson) {

  var topSites = sitesJson['Result'];
  var worldChart = dc.geoChoroplethChart("#world-chart")

  // var timeChart = 

  // crossfilter
  var ndx = crossfilter(topSites);

  // dimensions
  var ipDim = ndx.dimension(function(d) { 
    //if ( (typeof d["x-forwarded-for"] !== undefined) || (typeof d["x-forwarded-for"] !== null) )
    if ( "x-forwarded-for" in d ) {
      console.log("x forwarded: ", d["x-forwarded-for"]);
      return d["x-forwarded-for"];
    }
    //if (typeof d["remoteAddress"] !== undefined ) 
    if ( "remoteAddress" in d ) {
      console.log("remoteAddress: ", d["remoteAddress"].replace(/^.*:/, ''));
      return d["remoteAddress"].replace(/^.*:/, '')
    }
    console.log("nothing found!");
  });


  var dateDim = ndx.dimension(function(d) {
    if (typeof d['date'] !== 'undefined')
      return d['date'];
  });

  // metrics
  var totalIpsByCountry = ipDim.group().reduceCount(function(d) {
    if (typeof d["ips"][0] !== 'undefined' ) {
      if (typeof d["ips"][0]["country"] !== 'undefined' )
        return d["ips"][0]["country"];
  }});

  var max_country = totalIpsByCountry.top(1)[0].value;

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

  var minDate = dateDim.bottom(1)[0]["date"];
  var maxDate = dateDim.top(1)[0]["date"];

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
        //.colorDomain([0, max_country])
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
