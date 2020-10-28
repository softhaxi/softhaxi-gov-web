/* globals Chart:false, feather:false */

var data = [{
    label: 'WFO',
    backgroundColor: '#ffc107',
    data: [0, 4, 10, 5, 7, 9, 0]
}, {
    label: 'WFH',
    backgroundColor: '#17a2b8',
    data: [0, 7, 1, 7, 8, 5, 7]
}];

var options = {
  tooltips: {
      mode: 'label',
      callbacks: {
        label: function (tooltipItem, data) { 
          var type = data.datasets[tooltipItem.datasetIndex].label;
          var value = data.datasets[tooltipItem.datasetIndex].data[tooltipItem.index];
          var total = 0;
          for (var i = 0; i < data.datasets.length; i++)
            total += data.datasets[i].data[tooltipItem.index];
          if (tooltipItem.datasetIndex !== data.datasets.length - 1) {
            return type + " : " + value.toFixed(0).replace(/(\d)(?=(\d{3})+\.)/g, '1,');
          } else {
            return [type + " : " + value.toFixed(0).replace(/(\d)(?=(\d{3})+\.)/g, '1,'), "Total Absence : " + total];
          }
        }
      }
  },
  plugins: {
    datalabels: {
      formatter: function (value, ctx) {
        let sum = 0;
        let dataArr = ctx.chart.data.datasets[0].data;
        dataArr.map(data => {
          sum += data;
        });
        let percentage = (value * 100 / sum).toFixed(0) + "%";
        return percentage;
      },
      font: {
        weight: "bold"
      },
      color: "#fff"
    }
  },
  scales: {
    xAxes: [{
        stacked: true,
        gridLines: {
            display: false
        }
    }],
    yAxes: [{
      stacked: true,
      ticks: {
          beginAtZero: false,
          callback: function (value) {
              valuek = value;
              return valuek;
          }
      }
    }]
  }
};

(function () {
  'use strict'

  //feather.replace()

  // Graphs
  var ctx = document.getElementById('myChart').getContext("2d")
  // eslint-disable-next-line no-unused-vars
  ctx.height = 350;
  var myChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: [
        'Sunday',
        'Monday',
        'Tuesday',
        'Wednesday',
        'Thursday',
        'Friday',
        'Saturday'
      ],
      datasets: data
    },
    options: options
  })

  function initMap() {
    var location = new google.maps.LatLng(50.0875726, 14.4189987);
    var mapCanvas = document.getElementById('map');
    var mapOptions = {
        center: location,
        zoom: 16,
        panControl: false,
        mapTypeId: google.maps.MapTypeId.ROADMAP
    }
    var map = new google.maps.Map(mapCanvas, mapOptions);
  }

  google.maps.event.addDomListener(window, 'load', initMap);
})()