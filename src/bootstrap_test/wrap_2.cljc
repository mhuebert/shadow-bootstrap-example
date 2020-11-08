(ns bootstrap-test.wrap-2
  #?(:cljs (:require-macros bootstrap-test.wrap-2)))

(defmacro wrap [expr]
  `[:wrap-2 ~expr])
