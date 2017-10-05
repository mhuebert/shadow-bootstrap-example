(ns shadow-eval.core
  (:require

    ;; read
    [cljs.tools.reader.reader-types :as rt]
    [cljs.tools.reader :as r]

    ;; evaluate
    [cljs.js :as cljs]
    [shadow.bootstrap :as boot]

    ;; view
    [re-view.core :as v :refer [defview]]
    [lark.value-viewer.core :as views]))

(defonce state (atom {}))

(defonce _
         (boot/init #(swap! state assoc :ready true)))

(defn eval [s cb]
  (cljs/eval-str
    boot/compile-state-ref
    s
    "[test]"
    {:eval cljs/js-eval
     :load boot/load}
    cb))

(defview layout [{:keys [view/state]}]
  (if-not (:ready @state)
    [:div "Loading..."]
    [:div
     [:textarea.ba.b--gray.bw2.pa3.pre-wrap.ma3 {:value     (:input @state)
                                                 :on-change #(let [input (.. % -target -value)]
                                                               (swap! state assoc :input input)
                                                               (eval input (fn [{:keys [value error]}]
                                                                             (swap! state assoc :result (if error [:div "Error: " (str error)]
                                                                                                                  value)))))}]

     [:.pre-wrap.pa3.bg--near-white.ma3 (views/format-value (:result @state))]]))

(defn render []
  (v/render-to-dom (layout {:view/state state}) "shadow-eval"))


