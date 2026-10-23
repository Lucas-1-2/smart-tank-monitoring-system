Optional offline Chart.js location.

To run the dashboard without Internet access, place the Chart.js UMD build here as:

chart.umd.min.js

Then change index.html from the CDN script to:

<script src="lib/chart.umd.min.js"></script>

Otherwise:

<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js"></script>
