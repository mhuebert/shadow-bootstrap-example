(ns shadow-eval.core
  (:require

    ;; read
    [cljs.tools.reader.reader-types :as rt]
    [cljs.tools.reader :as r]

    ;; evaluate
    [cljs.js :as cljs]
    [shadow.cljs.bootstrap.browser :as boot]

    ;; view
    [re-view.core :as v :refer [defview]]
    [re-view-hiccup.core :refer [element]]
    [lark.value-viewer.core :as views]
    [re-db.d :as d]
    [re-db.patterns :as patterns]

    [cells.cell :as cell]
    [shapes.core :as shapes]
    ))

(defonce c-state (cljs/empty-state))

(defonce state (atom {:input "[(circle 40)\n (for [n (range 10)] n)\n (defcell x 10)]"}))

(defn eval-str [source cb]
  (cljs/eval-str
    c-state
    source
    "[test]"
    {:eval cljs/js-eval
     :load boot/load
     :ns   (symbol "shadow-eval.user")}
    cb))

(defn eval-to-page [source]
  (eval-str source #(swap! state assoc :result %)))

(defonce _
         (boot/init c-state
                    {:path         "/js/bootstrap"
                     :load-on-init '#{shadow-eval.user}}
                    (fn []
                      (swap! state assoc :ready true)
                      (eval-to-page (:input @state)))))

(defview layout [{:keys [view/state]}]
  (if-not (:ready @state)
    [:div "Loading..."]
    [:div
     [:textarea.ba.b--gray.bw2.pa3.pre-wrap.ma3 {:value     (:input @state)
                                                 :style     {:width  300
                                                             :height 150}
                                                 :on-change #(let [input (.. % -target -value)]
                                                               (swap! state assoc :input input)
                                                               (eval-to-page input))}]

     (let [{:keys [error value]} (:result @state)]
       [:.pre-wrap
        (if error (element [:.bg-pink.pa3
                            [:.b (ex-message error)]
                            [:div (str (ex-data error))]
                            (pr-str (ex-cause error))
                            ])
                  [:.bg-near-white.pa3 (views/format-value value)])])]))

(defn render []
  (v/render-to-dom (layout {:view/state state}) "shadow-eval"))

(extend-type cells.cell/Cell
  cells.cell/ICellStore
  (put-value! [this value]
    (d/transact! [[:db/add :cells (name this) value]]))
  (get-value [this]
    (d/get :cells (name this)))
  (invalidate! [this]
    (patterns/invalidate! d/*db* :ea_ [:cells (name this)]))
  lark.value-viewer.core/IView
  (view [this] (cells.cell/view this)))

(extend-protocol lark.value-viewer.core/IView
  Var
  (view [this] (@this)))

(extend-type shapes/Shape
  re-view-hiccup.core/IEmitHiccup
  (to-hiccup [this] (shapes/to-hiccup this)))

(extend-protocol cells.cell/IRenderHiccup
  object
  (render-hiccup [this] (re-view-hiccup.core/element this)))