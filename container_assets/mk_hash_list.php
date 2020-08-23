<?php

// documentation
// http://doc.ubuntu-fr.org/incron#prise_en_compte_de_la_configuration_en_redemarrant_le_demon
// http://manpages.ubuntu.com/manpages/focal/en/man5/incrontab.5.html

$dir = dirname(__FILE__);
$path_to_files = $dir . DIRECTORY_SEPARATOR . "files";
$files = scandir($path_to_files);
$list = [];

echo "<pre>".PHP_EOL;

// echo "md5       " . hash_file('md5', $file).PHP_EOL;
// echo "sha1      " . hash_file('sha1', $file).PHP_EOL;
// echo "ripemd160 " . hash_file('ripemd160', $file).PHP_EOL;
// echo "md2       " . hash_file("md2", $file).PHP_EOL;

foreach($files as $f) {
	$path = ($f != "." && $f != "..") ? $path_to_files . DIRECTORY_SEPARATOR . $f : "";

	clearstatcache();
	if(filesize($path)) {
		// the file is not empty
		$list[] = (pathinfo($path, PATHINFO_EXTENSION) == "utf8" || pathinfo($path, PATHINFO_EXTENSION) == "tmx") ? hash_file('md5', $path) . ":" . basename($path).PHP_EOL : "";
	}

	// same resutl with md5_file($f)
	// hash_file('md5', $f)
}

file_put_contents($dir . DIRECTORY_SEPARATOR . 'hash_list.txt', $list);
// $array = unserialize(file_get_contents('hast_list.txt')));
foreach($list as $l) { echo $l; }


?>
