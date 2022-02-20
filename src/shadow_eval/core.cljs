(ns shadow-eval.core
  (:require

    ;; evaluate
    [cljs.js :as cljs]
    [shadow.cljs.bootstrap.browser :as shadow.bootstrap]
    [shadow-eval.queue :as queue]

    ;; view
    [reagent.dom :as rdom]
    [reagent.core :as r]))

;; Source text to eval
(def examples
  [{:doc "fail - the macros-3 ns is in cljs + cljc namespaces - shadow bootstrap bug? "
    :source "(require-macros '[bootstrap-test.macros-3 :as m3])
  (m3/wrap-3 :x)"}]
  #_[{:doc "expected: the macro-ns `bootstrap-test.wrap-1` is never required with :require-macros"
    :source "(require-macros '[bootstrap-test.macros-2 :as m2])
  (m2/wrap-1 :x)"}
   ;; fail - cljs bug?
   {:doc "fail: from within a macros-ns, :require-macros is not sufficient to expose other macros. bug?"
    :source "(require-macros '[bootstrap-test.macros-2 :as m2])
  (m2/wrap-2 :x)"}

   ;; success
   {:doc "succeeds - the macro-ns is :require'd and also self-requires"
    :source "(require-macros '[bootstrap-test.macros-2 :as m2])
  (m2/wrap-3 :x)"}

   ;; success
   {:doc "succeeds - same as above but with separate cljs + cljc namespaces"
    :source "(require-macros '[bootstrap-test.macros-2 :as m2])
  (m2/wrap-4 :x)"}

   ;; fail - shadow bootstrap bug?
   {:doc "fail - the macros-3 ns is in cljs + cljc namespaces - shadow bootstrap bug? "
    :source "(require-macros '[bootstrap-test.macros-3 :as m3])
  (m3/wrap-3 :x)"}

   ;; fail - shadow bootstrap bug?
   {:doc "fail - likely same cause as above - cannot use reagent macros"
    :source "(require '[reagent.core :as r])
    (r/with-let [a 1] a)"}
   #_"(require '[userland.macros :as macros])
  (macros/current-ns-str)"

   ]
  #_[
     "^:hiccup [:b \"hello, world.\"]"
     "(for [n (range 10)] n)"
     "(defn greeting [name] (str \"hello, \" name))"
     "^:hiccup [greeting \"fido\"]"
     "(require '[reagent.core :as r] '[reagent.ratom :as ra])"
     "(defn counter []\n  (let [i (r/atom 0)\n        interval (js/setInterval #(swap! i inc) 500)]\n    (ra/add-on-dispose! reagent.ratom/*ratom-context* #(js/clearInterval interval))\n    (fn [] [:div @i])))"
     "^:hiccup [counter]"
     "(require '[cljs.js :as cljs])\n(fn? cljs/eval-str)"

     "(require-macros '[userland.macros-2 :as m2-macros])\n(m2-macros/no-op 2)"
     "(require '[userland.macros-2 :as m2])\n(m2/no-op 2)"
     ";; will not work, macros-3 is missing self-require\n(require-macros '[userland.macros-3 :as m3])\n(m3/no-op 3)"
     ])

;; Set up eval environment
(defonce c-state (cljs/empty-state))
(defonce !eval-ready? (r/atom false))



(defn eval* [source cb]
  (let [options {:eval cljs/js-eval
                 :verbose true
                 ;; use the :load function provided by shadow-cljs, which uses the bootstrap build's
                 ;; index.transit.json file to map namespaces to files.
                 :load (partial shadow.bootstrap/load c-state)
                 :context :expr}
        f (fn [x] (when (:error x)
                     (js/console.error (ex-cause (:error x))))
             (tap> x) (cb x))]
    (cljs/eval-str c-state (str source) "[test]" options f)))

(defonce eval-queue (new queue/FunctionQueue #queue[] false))

(defn eval! [source cb]
  (queue/conj! eval-queue
               (fn [done]
                 (eval* source
                        (fn [result]
                            (prn :source source)
                            (prn :result result)
                              (cb result)
                              (done))))))

(comment
  (tap> c-state))

;; Views

(defn example-view
  "Shows a single example, with an editable textarea and value-view."
  [{:keys [source doc]}]
  (r/with-let [!state (r/atom {:source source})
               _ (eval! source (partial swap! !state assoc :result))]
    (let [{:keys [source result]} @!state
          source-str (str source)]
      [:div.mb5.mt2

       [:div.flex
        [:div.bg-near-white.pa3.flex-none
         {:style {:width 450}}
         [:textarea.bn.pre-wrap.w-100.f7.lh-copy.bg-near-white.outline-0.code.overflow-auto
          {:value (str source-str)
           :style {:height (str (+ 1.75 (* 1.3125 (count (re-seq #"\n|\r\n" source-str)))) "rem")}
           :on-change #(let [next-source (.. % -target -value)]
                         (swap! !state assoc :source next-source)
                         (eval! next-source (partial swap! !state assoc :result)))}]]

        (let [{:keys [error value]} result]
          [:div.pre-wrap
           (if error
             [:div.pa3.bg-washed-red
              [:div.b (ex-message error)]
              [:div (str (ex-data error))]
              (pr-str (ex-cause error))]
             [:div.pa3
              (if (and (vector? value) (:hiccup (meta value)))
                value
                (pr-str value))])])]
       (when doc [:div.mh3.mv2 doc])])))

(defn examples-view
  "Root view for the page"
  []
  (if @!eval-ready?
    (into [:div.monospace.f6]
          (for [example examples]
            ^{:key (:source example)} [example-view example]))
    "Loading..."))

(defn render []
  (rdom/render [examples-view] (js/document.getElementById "shadow-eval")))

(defn ^:dev/after-load init []
  (shadow.bootstrap/init c-state
                         {:path "/js/bootstrap"
                          :load-on-init '#{shadow-eval.user}}
                         #(reset! !eval-ready? true))
  (render))
