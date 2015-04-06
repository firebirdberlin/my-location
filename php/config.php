<?php

//////////////////////////////////
/// CONFIGURATION OPTIONS
//////////////////////////////////

$res = array('status' => 'ok');

$DB_PATH = '/shares/Public/WWW/WWW-pub/location_tracker/data';


// set this option to true to enable persistent database connections; set to false for debugging
$DB_PERSISTENT = false;
$DB_NAME = "$DB_PATH/database.sqlite";
$CACERT_NAME = "$DB_PATH/cacert.pem";

if (substr(str_replace("\\", '/', __FILE__), -strlen($_SERVER['PHP_SELF'])) === $_SERVER['PHP_SELF']) {
    header('Content-Type: text/json');
    echo json_encode($res);
}


?>
