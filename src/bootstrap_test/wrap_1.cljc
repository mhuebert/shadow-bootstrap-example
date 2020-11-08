(ns bootstrap-test.wrap-1)

(defmacro wrap [expr]
  `[:wrap-1 ~expr])
