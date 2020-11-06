(ns userland.both)

(defmacro wrap [expr]
  `[::wrapped ~expr])
