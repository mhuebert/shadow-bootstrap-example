(ns shadow-eval.core
  (:require

    ;; evaluate
    [cljs.js :as cljs]
    [shadow.cljs.bootstrap.browser :as boot]

    ;; view
    [chia.view :as v]


    ;; things to eval and display
    [cells.cell :as cell]
    [shapes.core :as shapes]

    [clojure.string :as string]))


;; Source text to eval

(def source-examples ["(circle 40)"
                      "(for [n (range 10)] n)"
                      "(defcell x 10)"
                      "(cell (interval 100 inc))"
                      "(require '[cljs.js :as cljs])\n\n(fn? cljs/eval-str)"])

;; Set up eval environment

(defonce c-state (cljs/empty-state))
(defonce !eval-ready? (r/atom false))

(defn eval-str [source cb]
  (cljs/eval-str
    c-state
    source
    "[test]"
    {:eval cljs/js-eval
     :load (partial boot/load c-state)
     :ns   (symbol "shadow-eval.user")}
    cb))

;; Views

(v/defn show-example
  "Shows a single example, with an editable textarea and value-view."
  [source]
  (r/with-let [!state (r/atom {:source source})
               _ (eval-str source (partial swap! !state assoc :result))]
    (let [{:keys [source result]} @!state]
      [:.ma3.flex
       [:.bg-near-white.pa3.flex-none
        {:style {:width 450}}
        [:textarea.bn.pre.w-100.f6.lh-copy.bg-near-white.outline-0.monospace.overflow-auto
         {:value (:source @!state)
          :style {:height (str (+ 1.75 (* 1.3125 (count (re-seq #"\n|\r\n" source)))) "rem")}
          :on-change #(let [source (.. % -target -value)]
                        (swap! !state assoc :source source)
                        (eval-str source (partial swap! !state assoc :result)))}]]

       (let [{:keys [error value]} result]
         [:.pre-wrap
          (if error [:.pa3.bg-washed-red
                     [:.b (ex-message error)]
                     [:div (str (ex-data error))]
                     (pr-str (ex-cause error))]
                    [:.pa3 (str value)])])])))

(v/defn examples
  "Root view for the page"
  []
  (if-not @!eval-ready?
    "Loading..."
    (into [:.monospace.f6]
          (map show-example source-examples))))

(defn ^:dev/after-load init []
  (boot/init c-state
             {:path "/js/bootstrap"
              :load-on-init '#{shadow-eval.user}}
             #(reset! !eval-ready? true)))

(defn render []
  (v/render-to-dom (examples)
                   (js/document.getElementById "shadow-eval")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Protocol extensions to enable rendering of cells and shapes

(comment
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
    re-view.hiccup.core/IEmitHiccup
    (to-hiccup [this] (shapes/to-hiccup this)))

  (extend-protocol cells.cell/IRenderHiccup
    object
    (render-hiccup [this] (re-view.hiccup.core/element this))))
