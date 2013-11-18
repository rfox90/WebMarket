<?php
header("Access-Control-Allow-Origin: *");
include("XenForoAuth.conf.php");
define('XF_ROOT', $xf_root);
define('TIMENOW', time());
define('SESSION_BYPASS', false);
require_once(XF_ROOT . '/library/XenForo/Autoloader.php');
XenForo_Autoloader::getInstance()->setupAutoloader(XF_ROOT . '/library');
XenForo_Application::initialize(XF_ROOT . '/library', XF_ROOT);
XenForo_Application::set('page_start_time', TIMENOW);
XenForo_Application::disablePhpErrorHandler();
XenForo_Application::setDebugMode(false);
XenForo_Session::startPublicSession();
$visitor = XenForo_Visitor::getInstance();
if ($visitor->getUserId()) {
	$player;
	if ($custom_fields) {
		$player = $visitor['customFields'][$field_id];
	} else {
		$player = XenForo_Model::create('XenForo_Model_User')->getFullUserById($visitor->getUserId())['username'];
	}
	restore_error_handler();
	restore_exception_handler();
	$session = $_COOKIE[$cookie_prefix.'_session'];
	include("phpws/websocket.client.php");
	$ws = new WebSocket("ws://".$market_server.":".$market_port);
	$ws->open();
	$ws->send('{"req":3,"meta":{"name":"'.$player.'","password":"'.$session.'"},"data":{}}');
	$ws->close();
	echo '{"name":"'.$player.'","password":"'.$session.'"}';
}
?>