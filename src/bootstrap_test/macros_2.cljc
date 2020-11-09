(ns bootstrap-test.macros-2
  (:require [bootstrap-test.wrap-1 :as w1]
            [bootstrap-test.wrap-3 :as w3]
            [bootstrap-test.wrap-4 :as w4]
            #?(:clj [bootstrap-test.wrap-2 :as w2]))
  #?(:cljs (:require-macros bootstrap-test.macros-2
                            [bootstrap-test.wrap-2 :as w2])))

(defmacro wrap-1 [expr]
  ;; fails because bootstrap-test.wrap-1 does not self-require
  ;; `w1` is resolved but in this macro, `expr` is nil
  `(w1/wrap ~expr))

(defmacro wrap-2 [expr]
  ;; fails because bootstrap-test.wrap-2 is in :require-macros
  ;; `w2` is not resolved. (bootstrap-test.wrap-1 does self-require)
  `(w2/wrap ~expr))
;; =>
;; WARNING: No such namespace: w2, could not locate w2.cljs, w2.cljc, or JavaScript source providing "w2" at line 2
;; WARNING: Use of undeclared Var w2/wrap at line 2
;;
;; FAIL in (test-eval-str-with-transitive-macro-deps) (/Users/mattmini/Documents/projects/clojurescript/src/test/self/self_host/test.cljs:869:28)
;; expected: (nil? error)
;;   actual: (not (nil? #error {:message "ERROR", :data {:tag :cljs/analysis-error}, :cause #object[ReferenceError ReferenceError: w2 is not defined]}))

(defmacro wrap-3 [expr]
  ;; succeeds
  ;; wrap-3 self-requires and is in a :require form (not :require-macros)
  `(w3/wrap ~expr))

(defmacro wrap-4 [expr]
  ;; succeeds
  ;; wrap-4 has cljs + cljc files, the cljs file :require-macro's
  `(w4/wrap ~expr))
