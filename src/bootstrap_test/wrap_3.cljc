(ns bootstrap-test.wrap-3
  #?(:cljs (:require-macros bootstrap-test.wrap-3)))

(defmacro wrap [expr]
  `[:wrap-3 ~expr])
