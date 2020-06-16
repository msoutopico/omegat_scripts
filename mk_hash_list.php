<?php

$files = scandir('.');
$list = [];

echo "<pre>".PHP_EOL;

// echo "md5       " . hash_file('md5', $file).PHP_EOL;
// echo "sha1      " . hash_file('sha1', $file).PHP_EOL;
// echo "ripemd160 " . hash_file('ripemd160', $file).PHP_EOL;
// echo "md2       " . hash_file("md2", $file).PHP_EOL;

foreach($files as $f) {

	clearstatcache();
	if(filesize($f)) {
		// the file is not empty
		$list[] = (pathinfo($f, PATHINFO_EXTENSION) == "utf8" || pathinfo($f, PATHINFO_EXTENSION) == "tmx") ? hash_file('md5', $f) . ":" . $f.PHP_EOL : "";
	}

	// same resutl with md5_file($f)
}

file_put_contents('hash_list.txt', $list);
// $array = unserialize(file_get_contents('hast_list.txt')));
foreach($list as $l) { echo $l; }

?>
