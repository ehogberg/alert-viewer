(ns alert-viewer.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [alert-viewer.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'alert-viewer.core-test))
    0
    1))
