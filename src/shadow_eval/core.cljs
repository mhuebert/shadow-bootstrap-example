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
    [lark.value-viewer.core :as views]
    [re-db.d :as d]
    [re-db.patterns :as patterns]

    [cells.cell :as cell]
    [shapes.core :as shapes]
    ))

(defonce state (atom {:input "(cell \n (circle @(cell (interval 100 inc)))) "}))

(defn eval [s cb]
  (cljs/eval-str
    boot/compile-state-ref
    s
    "[test]"
    {:eval cljs/js-eval
     :load (fn [& args]
             (prn :load args)
             (apply boot/load args))
     :ns   (symbol "shadow-eval.user")}
    cb))

(defonce _
         (boot/init
           #(eval (str '(require '[shadow-eval.user :include-macros true]))
                  (fn [] (swap! state assoc :ready true)))))


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