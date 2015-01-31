(ns core-test
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:require [cemerick.cljs.test :as t]
            [iframe-app.core :refer [main]]))

(enable-console-print!)

(set! (.-onload js/window)
      (fn []
        (.appendChild (.-body js/document)
                      (let [element (.createElement js/document "div")
                            _ (set! (.-id element) "main")]
                        element))
        (t/run-all-tests)
        (.callPhantom js/window "exit")))



(deftest javascript-allows-div0
         (is (= js/Infinity (/ 1 0) (/ (int 1) (int 0)))))
