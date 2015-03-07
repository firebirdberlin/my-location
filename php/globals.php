<?php
require_once 'config.php';
require_once 'MCrypt.php';

function DEBUG($msg) {
    $timestamp = date('D M j H:i:s.u Y');
    file_put_contents('php://stdout', "[$timestamp] [ctlon:debug] $msg\n");
}

function haversineDistance($lat1, $lng1, $lat2, $lng2) {
  $dLat = 0.5 * deg2rad($lat2 - $lat1);
  $dLng = 0.5 * deg2rad($lng2 - $lng1);
  $a = sin($dLat) * sin($dLat) + cos(deg2rad($lat1)) * cos(deg2rad($lat2)) * sin($dLng) * sin($dLng);
  $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
  return 1000 * 6371.0 * $c;
}

function bearing($lat1, $lng1, $lat2, $lng2) {
  $lat1 = deg2rad($lat1);
  $lat2 = deg2rad($lat2);
  $dLng = deg2rad($lng2 - $lng1);
  $y = sin($dLng) * cos($lat2);
  $x = cos($lat1) * sin($lat2) - sin($lat1) * cos($lat2) * cos($dLng);
  return rad2deg(atan2($y, $x));
}

$T0 = microtime(true);
function processingTime() {
    global $T0;
    $dt = round(microtime(true) - $T0, 3);
    return ($dt < 0.001) ? '<1ms' : '~' . $dt . 's';
}

$dbh = new PDO("sqlite:$DB_NAME", null, null, array(
     PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
     PDO::ATTR_PERSISTENT => $DB_PERSISTENT
));

?>
