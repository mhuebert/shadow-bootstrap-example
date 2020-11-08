(ns bootstrap-test.wrap-4)

(defmacro wrap [expr]
  `[:wrap-4 ~expr])
