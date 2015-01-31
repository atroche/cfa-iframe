// reusable slimerjs/phantomjs script for running clojurescript.test tests
// see http://github.com/cemerick/clojurescript.test for more info

console.log("runner.js running");
var p = require('webpage').create();
var fs = require('fs');
var sys = require('system');

// this craziness works around inscrutable JS context issues when tests being
// run use iframes and such; rather than injecting or eval'ing test scripts and
// expressions, dump them all into a static HTML file and everything will be
// guaranteed to work.
var loadsLibraries = p.evaluate(function() {
	    return (typeof cljs !== undefined);
	});

console.log(sys.args);
console.log('huh');
var html = "";
var pagePath = sys.args[0] + ".html";

console.log(pagePath);

for (var i = 1; i < 2; i++) {
    var src;

    if (fs.exists(sys.args[i])) {
	src = fs.read(sys.args[i]);
    }

    html += "<script>//<![CDATA[\n" + src + "\n//]]></script>";
}

html = "<html><head>" + html + "</head><body>hey</body></html>";
fs.write(pagePath, html, 'w');

	p.onConsoleMessage = function(message) {
    		console.log("INSIDE PAGE: " + message);
    	};

	p.onLoadFinished = function(){
		p.evaluate(function(){console.log('i am inside the page')});
		console.log('load finished');
	}

			p.onCallback = function (x) {
    		var line = x.toString();
    		if (line !== "[NEWLINE]") {
    			console.log(line.replace(/\[NEWLINE\]/g, "\n"));
    		}}


		p.onError = function(msg) {

			console.error(msg);
				phantom.exit(1);
		};

p.open("file://" + "/Users/aroche/Code/clj/iframe-app/resources/runner.js.html", function () {
	    fs.remove(pagePath);
		console.log('this all seems fine');




	p.evaluate(function(){console.log('i am also inside the page but not loaded')});
	p.evaluate(function(){console.log(cemerick)});




//

//    };
//
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