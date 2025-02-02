package com.btc.tsobss.samples.endpoint;

import com.btc.tsobss.samples.order.OrderData;
import com.btc.tsobss.samples.order.webservice.GetOrderRequest;
import com.btc.tsobss.samples.order.webservice.GetOrderResponse;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class OrderEndpoint {

    public static final String NAME = "xsd/order";

    public static final String ROOT_ELEMENT_NAMESPACE = "http://samples.tsobss.btc.com/order/webservice";

    public static final String ROOT_ELEMENT = "GetOrderRequest";

    @SuppressWarnings("unused")
    @PayloadRoot(namespace = ROOT_ELEMENT_NAMESPACE, localPart = ROOT_ELEMENT)
    @ResponsePayload
    public GetOrderResponse getData(@RequestPayload GetOrderRequest request) {
        GetOrderResponse response = new GetOrderResponse();
        OrderData data = new OrderData();
        data.setName("Example Name " + request.getId());
        data.setValue("Example Value");
        response.setData(data);
        return response;
    }
}