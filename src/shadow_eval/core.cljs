(ns shadow-eval.core
  (:require

    ;; evaluate
    [cljs.js :as cljs]
    [shadow.cljs.bootstrap.browser :as shadow.bootstrap]

    ;; view
    [reagent.dom :as rdom]
    [reagent.core :as r]))

;; Source text to eval
(def source-examples
  [
   "^:hiccup [:b \"hello, world.\"]"
   "(for [n (range 10)] n)"
   "(defn greeting [name] (str \"hello, \" name))"
   "^:hiccup [greeting \"fido\"]"
   "(require '[reagent.core :as r] '[reagent.ratom :as ra])"
   "(defn counter []\n  (let [i (r/atom 0)\n        interval (js/setInterval #(swap! i inc) 500)]\n    (ra/add-on-dispose! reagent.ratom/*ratom-context* #(js/clearInterval interval))\n    (fn [] [:div @i])))"
   "^:hiccup [counter]"
   "(require '[cljs.js :as cljs])\n(fn? cljs/eval-str)"

   "(require '[userland.macros :as macros])
  (macros/current-ns-str)"
   "(require-macros '[userland.macros-2 :as m2-macros])\n(m2-macros/no-op 2)"
   "(require '[userland.macros-2 :as m2])\n(m2/no-op 2)"

   ";; will not work, macros-3 is missing self-require\n(require-macros '[userland.macros-3 :as m3])\n(m3/no-op 3)"
   ])

;; Set up eval environment
(defonce c-state (cljs/empty-state))
(defonce !eval-ready? (r/atom false))

(defn eval-str [source cb]
  (cljs/eval-str
    c-state
    source
    "[test]"
    {:eval cljs/js-eval
     ;; use the :load function provided by shadow-cljs, which uses the bootstrap build's
     ;; index.transit.json file to map namespaces to files.
     :load (partial shadow.bootstrap/load c-state)
     :context :expr}
    cb))

;; Views

(defn show-example
  "Shows a single example, with an editable textarea and value-view."
  [source]
  (r/with-let [!state (r/atom {:source source})
               _ (eval-str source (partial swap! !state assoc :result))]
    (let [{:keys [source result]} @!state]
      [:div.ma3.flex
       [:div.bg-near-white.pa3.flex-none
        {:style {:width 450}}
        [:textarea.bn.pre.w-100.f6.lh-copy.bg-near-white.outline-0.monospace.overflow-auto
         {:value (:source @!state)
          :style {:height (str (+ 1.75 (* 1.3125 (count (re-seq #"\n|\r\n" source)))) "rem")}
          :on-change #(let [source (.. % -target -value)]
                        (swap! !state assoc :source source)
                        (eval-str source (partial swap! !state assoc :result)))}]]

       (let [{:keys [error value]} result]
         [:div.pre-wrap
          (if error
            (do
              (js/console.error (ex-cause error))
              [:div.pa3.bg-washed-red
               [:div.b (ex-message error)]
               [:div (str (ex-data error))]
               (pr-str (ex-cause error))])
            [:div.pa3
             (if (and (vector? value) (:hiccup (meta value)))
               value
               (pr-str value))])])])))

(defn examples
  "Root view for the page"
  []
  (if @!eval-ready?
    (into [:div.monospace.f6]
          (for [source source-examples]
            ^{:key source} [show-example source]))
    "Loading..."))

(defn render []
  (rdom/render [examples] (js/document.getElementById "shadow-eval")))

(defn ^:dev/after-load init []
  (shadow.bootstrap/init c-state
                         {:path "/js/bootstrap"
              :load-on-init '#{shadow-eval.user}}
                         #(reset! !eval-ready? true))
  (render))
