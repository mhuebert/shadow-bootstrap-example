(ns bootstrap-test.sub-macros
  #?(:cljs (:require-macros bootstrap-test.sub-macros)))

(defmacro wrap [expr]
  `[:wrap ~expr])
