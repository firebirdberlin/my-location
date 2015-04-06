<?php

require_once 'globals.php';

if (!isset($_REQUEST['msg'])) {
    $res['status'] = 'error';
    $res['error'] = 'Invalid msg' . $_REQUEST['msg'];
    goto end;
}
$msg = $_REQUEST['msg'];

$timestamp = isset($_REQUEST['timestamp']) ? $_REQUEST['timestamp'] : time();
if (!preg_match('/^\\d+$/', $timestamp)) {
    $res['status'] = 'error';
    $res['error'] = 'Ungültiger Zeitstempel: ' . $timestamp;
    goto end;
}
$timestamp = intval($timestamp);

$accuracy = isset($_REQUEST['accuracy']) ? intval($_REQUEST['accuracy']) : null;
$altitude = isset($_REQUEST['altitude']) ? floatval($_REQUEST['altitude']) : null;
$altitudeaccuracy = isset($_REQUEST['altitudeaccuracy']) ? intval($_REQUEST['altitudeaccuracy']) : null;
$heading = isset($_REQUEST['heading']) ? floatval($_REQUEST['heading']) : null;
$speed = isset($_REQUEST['speed']) ? floatval($_REQUEST['speed']) : null;
$mcrypt = new MCrypt();

$data = array();
$msg = $mcrypt->decrypt($msg);
$values = explode('|',$msg);
foreach($values as $val) {
    $pair = split('=', $val);
    $data[$pair[0]] = $pair[1];
}

if ($dbh) {
    $sth = $dbh->prepare('REPLACE INTO `locations` (`userid`, `timestamp`, `lat`, `lng`, `accuracy`, `altitude`, `altitudeaccuracy`, `heading`, `speed`) VALUES(?,?,?,?,?,?,?,?,?)');
    $sth->execute(array($data['user_id'], $timestamp, $data['lat'], $data['lon'], $accuracy, $altitude, $altitudeaccuracy, $heading, $speed));
    $res['id'] = $dbh->lastInsertId();
    $res['status'] = 'ok';
    $res['userid'] = $data['user_id'];
    $res['lat'] = $data['lat'];
    $res['lng'] = $data['lon'];
    $res['timestamp'] = $timestamp;
    $res['processing_time'] = processingTime();
    $res['msg'] = $msg;
}
else {
    $res['status'] = 'error';
    $res['error'] = $dbh->errorInfo();
}

end:
header('Content-Type: text/json');
echo json_encode($res);
?>
