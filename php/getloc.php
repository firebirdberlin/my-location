<?php

require_once 'globals.php';

$data = array();
if (isset($_REQUEST['msg'])) {
	$msg = $_REQUEST['msg'];

	$mcrypt = new MCrypt();
	$msg = $mcrypt->decrypt($msg);
	$values = explode('|',$msg);
	foreach($values as $val) {
	    $pair = split('=', $val);
	    $data[$pair[0]] = $pair[1];
	}
} else {
	$res['status'] = 'error';
    $res['error'] = 'Invalid msg' . $_REQUEST['msg'];
    goto end;
}

$userids = $data['userids'];
$userids = split(',', $userids);

$with_avatar = isset($_REQUEST['avatar']) && $_REQUEST['avatar'] === 'true';
$as_array = isset($_REQUEST['as_array']) && $_REQUEST['as_array'] === 'true';

$maxage = (isset($_REQUEST['maxage']) && is_numeric($_REQUEST['maxage']))
    ? intval($_REQUEST['maxage'])
    : time();
$t0 = time() - $maxage;

if (isset($_REQUEST['lat']))
    $reflat = floatval($_REQUEST['lat']);
if (isset($_REQUEST['lng']))
    $reflng = floatval($_REQUEST['lng']);
if (isset($_REQUEST['maxdist']))
    $maxdist = floatval($_REQUEST['maxdist']);

$checkdist = isset($reflat) && isset($reflng) && isset($maxdist);

if ($dbh) {
    $location_query = $dbh->prepare("SELECT `timestamp`, `lat`, `lng`, `accuracy`, `altitude`, `altitudeaccuracy`, `heading`, `speed`" .
        " FROM `locations`" .
        " WHERE `userid` = :userid" .
        " ORDER BY `timestamp` DESC" .
        " LIMIT 1");

    foreach ($userids as $buddy) {
        $location_query->execute(array($buddy));

        foreach($location_query->fetchAll() as $row)  {
            $lat = floatval($row[1]);
            $lng = floatval($row[2]);
            if ($checkdist && haversineDistance($reflat, $reflng, $lat, $lng) > $maxdist)
                continue;

        $data = array(
                  'timestamp' => intval($row[0]),
                  'lat' => $lat,
                  'lng' => $lng,
                  'accuracy' => floatval($row[3]),
                  //'altitude' => floatval($row[4]),
                  //'altitudeaccuracy' => floatval($row[5]),
                  //'heading' => floatval($row[6]),
                  //'speed' => floatval($row[7])
              );
        if ($as_array)
              $res['users'][] = $data;
            else
              $res['users'][$buddy] = $data;
        }
        if ($with_avatar)
            $res['users'][$buddy]['avatar'] = $buddy[2];
    }
    $mcrypt = new MCrypt();
    $data = json_encode($res['users']);
    unset($res['users']);
    $res['data'] = $mcrypt->encrypt($data);
    $res['status'] = 'ok';
    $res['processing_time'] = processingTime();
}

end:
header('Content-Type: text/json');
echo json_encode($res);
?>


