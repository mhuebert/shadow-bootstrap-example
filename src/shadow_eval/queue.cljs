(ns shadow-eval.queue
  (:refer-clojure :exclude [conj!]))

(defprotocol IQueue
  (maybe-eval! [this])
  (conj! [this f]))

(deftype FunctionQueue [^:mutable queue ^:mutable running?]
  IQueue
  (maybe-eval! [this]
    (when-not running?
      (when-let [f (peek queue)]
        (set! running? true)
        (set! queue (pop queue))
        (f (fn []
             (set! running? false)
             (maybe-eval! this))))))
  (conj! [this f]
    (set! queue (conj queue f))
    (maybe-eval! this)))
