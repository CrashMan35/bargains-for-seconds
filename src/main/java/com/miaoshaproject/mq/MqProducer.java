package com.miaoshaproject.mq;


import com.alibaba.fastjson.JSON;
import com.miaoshaproject.dao.StockLogDoMapper;
import com.miaoshaproject.dataobject.StockLogDo;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.OrderService;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

@Component
public class MqProducer {

    private DefaultMQProducer producer;

    private TransactionMQProducer transactionMQProducer;

    @Value("${mq.nameserver.addr}")
    private String nameAddr;

    @Value("${mq.topicname}")
    private String topicName;

    @Autowired
    private OrderService orderService;

    @Autowired
    private StockLogDoMapper stockLogDoMapper;

    @PostConstruct
    public void init() throws MQClientException {

        //做mqProducer的初始化
        producer = new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(nameAddr);
        producer.start();

        transactionMQProducer = new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(nameAddr);
        transactionMQProducer.start();

        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object org) {
                //真正要做的事 创建订单
                Integer itemId = (Integer) ((Map) org).get("itemId");
                Integer userId = (Integer) ((Map) org).get("userId");
                Integer promoId = (Integer) ((Map) org).get("promoId");
                Integer amount = (Integer) ((Map) org).get("amount");
                String stockLogId = (String) ((Map) org).get("stockLogId");

                try {
                    orderService.createOrder(userId, itemId, promoId, amount,stockLogId);
                } catch (BusinessException e) {
                    e.printStackTrace();
                    //设置对应的stockLog为回滚状态
                    StockLogDo stockLogDo = stockLogDoMapper.selectByPrimaryKey(stockLogId);
                    stockLogDo.setStatus(3);
                    stockLogDoMapper.updateByPrimaryKeySelective(stockLogDo);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                //根据是否扣减成功,来判断要返回COMMIT,ROLLBACK还是继续UNKNOWN
                String jsonString = new String(msg.getBody());
                Map<String, Object> map = JSON.parseObject(jsonString, Map.class);
                Integer itemId = (Integer) map.get("itemId");
                Integer amount = (Integer) map.get("amount");
                String stockLogId = (String) map.get("stockLogId");
                StockLogDo stockLogDo = stockLogDoMapper.selectByPrimaryKey(stockLogId);
                if (stockLogDo == null) {
                    return LocalTransactionState.UNKNOW;
                }
                if (stockLogDo.getStatus().intValue() == 2) {
                    return LocalTransactionState.COMMIT_MESSAGE;
                } else if (stockLogDo.getStatus().intValue() == 1) {
                    return LocalTransactionState.UNKNOW;
                }
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }

        });
    }

    //事务型同步扣减消息
    public boolean transactionAsyncReduceStock(Integer userId,Integer promoId,Integer itemId, Integer amount,String stockLogId) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        bodyMap.put("stockLogId", stockLogId);

        Map<String, Object> argsMap = new HashMap<>();
        argsMap.put("itemId", itemId);
        argsMap.put("amount", amount);
        argsMap.put("userId", userId);
        argsMap.put("promoId", promoId);
        argsMap.put("stockLogId", stockLogId);

        Message message = new Message(topicName, "increase",
                JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        TransactionSendResult sendResult = null;
        try {
            sendResult=transactionMQProducer.sendMessageInTransaction(message, argsMap);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        }
        if (sendResult.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE) {
            return false;
        } else if (sendResult.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE) {
            return true;
        } else {
            return false;
        }

    }

    //同步库存扣减消息
    public boolean asyncReduceStock(Integer itemId, Integer amount) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        Message message = new Message(topicName, "increase",
                JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        try {
            producer.send(message);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        } catch (RemotingException e) {
            e.printStackTrace();
            return false;
        } catch (MQBrokerException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
