(ns shadow-eval.core
  (:require

    ;; evaluate
    [cljs.js :as cljs]
    [shadow.cljs.bootstrap.browser :as boot]

    ;; view
    [reagent.dom :as rdom]
    [reagent.core :as r]))

(defn counter []
  (let [i (r/atom 0)
        interval (js/setInterval #(swap! i inc) 1000)]
    (reagent.ratom/add-on-dispose! reagent.ratom/*ratom-context* #(js/clearInterval interval))
    (fn [] [:div @i])))

;; Source text to eval
(def source-examples ["(for [n (range 10)] n)"
                      "(defn greeting [name] (str \"hello, \" name))\n[greeting \"fido\"]"
                      "(require '[reagent.core :as r] '[reagent.ratom :as ra])\n\n(defn counter []\n  (let [i (r/atom 0)\n        interval (js/setInterval #(swap! i inc) 500)]\n    (ra/add-on-dispose! reagent.ratom/*ratom-context* #(js/clearInterval interval))\n    (fn [] [:div @i])))\n[counter]"
                      "(require '[cljs.js :as cljs])\n\n(str (fn? cljs/eval-str))"
                      "(require-macros '[macro-example.core :as macros])\n\n(macros/current-ns-str)"
                      ])

;; Set up eval environment

(defonce c-state (cljs/empty-state))
(defonce !eval-ready? (r/atom false))
(keys (get-in @c-state [:cljs.analyzer/namespaces 'macro-example.core$macros]))
(defn eval-str [source cb]
  (cljs/eval-str
    c-state
    source
    "[test]"
    {:eval cljs/js-eval
     :load (partial boot/load c-state)
     :ns   (symbol "shadow-eval.user")}
    #(do (prn :evaluated %) (cb %))))

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
          (if error [:div.pa3.bg-washed-red
                     [:div.b (ex-message error)]
                     [:div (str (ex-data error))]
                     (pr-str (ex-cause error))]
                    [:div.pa3 value])])])))

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
  (boot/init c-state
             {:path "/js/bootstrap"
              :load-on-init '#{shadow-eval.user}}
             #(reset! !eval-ready? true))
  (render))
