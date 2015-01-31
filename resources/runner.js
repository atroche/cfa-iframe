// reusable slimerjs/phantomjs script for running clojurescript.test tests
// see http://github.com/cemerick/clojurescript.test for more info


var p = require('webpage').create();
var fs = require('fs');
var sys = require('system');


var html = "";
var pagePath = sys.args[0] + ".html";

for (var i = 1; i < 2; i++) {
    var src;

    if (fs.exists(sys.args[i])) {
	src = fs.read(sys.args[i]);
    }

    html += "<script>//<![CDATA[\n" + src + "\n//]]></script>";
}

html = "<html><head>" + html + "</head><body></body></html>";
fs.write(pagePath, html, 'w');

	p.onConsoleMessage = function(message) {
    		console.log(message);
    	};

			p.onCallback = function (x) {
    		var line = x.toString();
    		if (line !== "[NEWLINE]") {
    			console.log(line.replace(/\[NEWLINE\]/g, "\n"));
    		}};


		p.onError = function(msg) {

			console.error(msg);
				phantom.exit(1);
		};

p.open("file://" + "/Users/aroche/Code/clj/iframe-app/resources/runner.js.html", function () {
	    fs.remove(pagePath);

    // p.evaluate is sandboxed, can't ship closures across;
    // so, a bit of a hack, better than polling :-P
    var exitCodePrefix = "phantom-exit-code:";
    p.onAlert = function (msg) {
	var exit = msg.replace(exitCodePrefix, "");
	if (msg != exit) phantom.exit(parseInt(exit));
    };
//
    p.evaluate(function (exitCodePrefix) {
	var results = cemerick.cljs.test.run_all_tests();
	//console.log(results);
	cemerick.cljs.test.on_testing_complete(results, function () {
	    window.alert(exitCodePrefix +
			 (cemerick.cljs.test.successful_QMARK_(results) ? 0 : 1));
	});
    }, exitCodePrefix);

});