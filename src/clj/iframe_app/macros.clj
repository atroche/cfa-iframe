(ns iframe-app.macros)

(defmacro wait-a-bit [& milliseconds]
  (let [milliseconds (or (first milliseconds) 50)]
    `(let [wait-chan# (cljs.core.async/chan)]
       (js/setTimeout (fn [] (cljs.core.async/put! wait-chan# true)) ~milliseconds)
       (cljs.core.async/<! wait-chan#))))

