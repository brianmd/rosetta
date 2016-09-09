(println "loading summit.sap.order")

(ns summit.sap.order
  (:require [summit.sap.core :refer :all]
            [clojure.string :as str]
            [summit.utils.core :as utils :refer [->int as-document-num examples ppn]]
            ))

(defn transform-address [address]
  {:id       (->int (:address-code address))
   :name     (:name address)
   :name2    (:name2 address)
   :city     (:city address)
   :county   (:county address)
   :state    (:state address)
   :street   (:street address)
   :street2  (:street2 address)
   :po-box   (:po-box address)
   :zip-code (:zip-code address)
   :country  (:country address)})

(defn transform-comment [comment]
  (let [order-id (->int (:order comment))]
    (let [comment-id (->int (:item comment))]
      {:id    (str order-id "-" comment-id)
       :value (:tdline comment)})))

(defn transform-line-item [line-item]
  (let [order-id (->int (:order line-item))]
    (let [line-item-id (->int (:item line-item))]
      {:id              (str order-id "-" line-item-id)
       :account-id      (->int (:customer line-item))
       :job-account-id  (->int (:job-account line-item))
       :order-id        order-id
       :product-id      (->int (:material line-item))
       :delivery-status (:cust-cmpl-status line-item)
       :extended-price  0
       :quantity        0
       :price           0
       :shipping-type   (:shipping-type line-item)})))

(defn transform-order-summary [order]
  {:id               (->int (:order order))
   :account-id       (->int (:customer order))
   :job-account-id   (->int (:job-account order))
   :bill-address-id  (->int (:bill-address-code order))
   :pay-address-id   (->int (:pay-address-code order))
   :ship-address-id  (->int (:ship-address-code order))
   :sold-address-id  (->int (:sold-address-code order))
   :created-at       (:created-date order)
   :expected-at      (:cust-expected order)
   :job-account-name (:job-account-name order)
   :line-item-count  (:number-of-items order)
   :purchase-order   (:cust-po order)
   :shipping-type    (:shipping-type order)
   :status           (:cust-cmpl-status order)
   :sub-total        (:subtotal order)
   :tax              (:sales-tax order)
   :total            (:total-cost order)})

(defn transform-addresses [addresses]
  (ppn (str "transform-addresses"))
  (map transform-address addresses))

(defn transform-comments [comments]
  (ppn (str "transform-comments"))
  (map transform-comment comments))

(defn transform-line-items [line-items]
  (ppn (str "transform-order-detail"))
  (map transform-line-item line-items))

(defn transform-orders-summary [orders]
  (ppn (str "transform-orders-summary"))
  (map transform-order-summary orders))

(defn retrieve-maps [order-fn]
  (let [attr-defs (partition 3
                             [:addresses :et-addresses transform-addresses
                              :comments :et-text transform-comments
                              :line-items :et-orders-detail transform-line-items
                              :orders :et-orders-summary transform-orders-summary])]
    (into {}
          (for [attr-def attr-defs]
            (let [web-name          (first attr-def)
                  sap-name          (second attr-def)
                  name-transform-fn (nth attr-def 2)]
              [web-name (name-transform-fn (pull-map order-fn sap-name))])))))

(defn- collect-addresses-for-order [order addresses]
  (let [ids (list
             (:bill-address-id order)
             (:ship-address-id order)
             (:sold-address-id order)
             (:pay-address-id order))]
    (filter (fn [x] (some #(= (:id x) %) ids) addresses))))

(defn transform-order [maps order]
  (let [id (:id order)]
    {:order order
     :addresses (collect-addresses-for-order order (:addresses maps))}))

(defn transform-orders [order-fn]
  (let [maps (retrieve-maps order-fn)]
    (let [orders (:orders maps)]
      (map (partial transform-order maps) orders))))

(defn order
  ([order-id] (order :qas order-id))
  ([system order-id]
    (utils/ppn (str "getting order " order-id " on " system))
    (let [order-fn (find-function system :Z_O_ORDERS_QUERY) id-seq-num (atom 0)]
      (push order-fn {
        ; :i-customer "0001002225"
        :i-order (as-document-num order-id)
        :if-orders "X"
        :if-details "X"
        :if-addresses "X"
        :if-texts "X"})
      (execute order-fn)
      ;; (transform-order order-fn))))
      order-fn)))
      ; (execute order-fn)
      ; (let [result (transform-order order-fn)]
      ;   (if result (assoc-in result [:data :id] order-id))))))
; (def x (order "3970176"))
; (order "3970176")
(def f (order "3970176"))

;; (pull-map f :et-orders-detail)
;; (ppn (retrieve-maps f))
(ppn (transform-orders f))
;; (ppn (:line-items (retrieve-maps f))) 
;; (retrieve-maps f)
;; (ppn (pull-map (utils/ppl "function" f) :et-orders-detail))
;; (keys f)
(ppn (function-interface f))
;; (ppn (keys (function-interface f)))

(println "done loading summit.sap.order")
