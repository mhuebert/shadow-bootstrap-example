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
  [{:doc "fail - the macros ns is in cljs + cljc namespaces "
    :source "(require-macros '[bootstrap-test.macros :as macros])
             (macros/wrap :x)"}]
  )

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
