(ns shadow-eval.queue
  (:refer-clojure :exclude [conj!]))

(defprotocol IQueue
  (maybe-eval! [this])
  (conj! [this f]))

;; queue for evaluating async functions sequentially.
;; conj! a function to the queue, it will receive a `done` callback
;; which must be called for the queue to process a next item.
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
