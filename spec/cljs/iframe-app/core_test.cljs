(ns core-test
    (:require-macros [cemerick.cljs.test
                      :refer (is deftest with-test run-tests testing test-var)])
    (:require [cemerick.cljs.test :as t]
              [iframe-app.core]))

(enable-console-print!)

(deftest javascript-allows-div0
         (is (= js/Infinity (/ 1 0) (/ (int 1) (int 0)))))

(println "is this working??! hah lol 2")
