import http from './index'

/** 执行交易链路（下单→库存→支付） */
export function executeOrder(userId: string, productId: string, amount: number) {
  return http.post('/order/execute', null, { params: { userId, productId, amount } }).then(r => r.data)
}
