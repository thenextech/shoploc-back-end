package nextech.shoploc.services.orderLine;

import nextech.shoploc.domains.Order;
import nextech.shoploc.domains.OrderLine;
import nextech.shoploc.domains.Product;
import nextech.shoploc.models.order_line.OrderLineRequestDTO;
import nextech.shoploc.models.order_line.OrderLineResponseDTO;
import nextech.shoploc.models.product.ProductResponseDTO;
import nextech.shoploc.repositories.OrderLineRepository;
import nextech.shoploc.repositories.OrderRepository;
import nextech.shoploc.repositories.ProductRepository;
import nextech.shoploc.services.product.ProductService;
import nextech.shoploc.utils.ModelMapperUtils;
import nextech.shoploc.utils.exceptions.NotFoundException;
import org.modelmapper.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderLineServiceImpl implements OrderLineService {

    private final OrderLineRepository orderLineRepository;
    private final OrderRepository orderRepository;

    private final ModelMapperUtils modelMapperUtils;
    
    @Autowired
    private ProductService productService;

    public OrderLineServiceImpl(OrderLineRepository orderLineRepository,
                                OrderRepository orderRepository, ProductRepository productRepository, ModelMapperUtils modelMapperUtils) {
        this.orderLineRepository = orderLineRepository;
        this.orderRepository = orderRepository;
        this.modelMapperUtils = modelMapperUtils;
        // Mapper & Converter
        Converter<Long, Product> convertIdentifierToProduct = context -> productRepository.findById(context.getSource())
                .orElseThrow(() -> new NotFoundException("Product not found with ID: " + context.getSource()));
        Converter<Long, Order> convertIdentifierToOrder = context -> this.orderRepository.findById(context.getSource())
                .orElseThrow(() -> new NotFoundException("Order not found with ID: " + context.getSource()));


        // OrderLineRequestDTO -> OrderLine
        this.modelMapperUtils.getModelMapper().typeMap(OrderLineRequestDTO.class, OrderLine.class).addMappings(mapper -> {
            mapper.when(ctx -> ctx.getSource() != null)
                    .using(convertIdentifierToOrder)
                    .map(OrderLineRequestDTO::getOrderId, OrderLine::setOrder);
            mapper.when(ctx -> ctx.getSource() != null)
                    .using(convertIdentifierToProduct)
                    .map(OrderLineRequestDTO::getProductId, OrderLine::setProduct);
        });

        // OrderLine -> OrderLineResponseDTO
        this.modelMapperUtils.getModelMapper().typeMap(OrderLine.class, OrderLineResponseDTO.class).addMappings(mapper -> {
            mapper.map(src -> src.getProduct().getProductId(), OrderLineResponseDTO::setProductId);
            mapper.when(ctx -> ctx.getSource() != null)
                    .map(src -> src.getOrder().getOrderId(), OrderLineResponseDTO::setOrderId);
        });

    }

    @Override
    public OrderLineResponseDTO createOrderLine(OrderLineRequestDTO orderLineRequestDTO) {
        // Fetch the Product entity using productId
        ProductResponseDTO product = productService.getProductById(orderLineRequestDTO.getProductId());
        // Create the OrderLine entity
        OrderLine orderLine = modelMapperUtils.getModelMapper().map(orderLineRequestDTO, OrderLine.class);
        
        // Set the productName into OrderLineResponseDTO
        OrderLineResponseDTO orderLineResponseDTO = modelMapperUtils.getModelMapper().map(orderLine, OrderLineResponseDTO.class);
        orderLineResponseDTO.setProductName(product.getName());
        
        // Save the OrderLine entity
        orderLine = orderLineRepository.save(orderLine);
        return orderLineResponseDTO;
    }


    @Override
    public OrderLineResponseDTO getOrderLineById(Long id) {
        OrderLine orderLine = orderLineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("OrderLine not found with ID: " + id));

        return modelMapperUtils.getModelMapper().map(orderLine, OrderLineResponseDTO.class);
    }
    
    @Override
    public List<OrderLineResponseDTO> getOrderLinesByMerchantId(Long merchantId) {

        List<OrderLine> orderLines = orderLineRepository.findAllByMerchantId(merchantId);

        return orderLines.stream()
                .map(orderLine -> modelMapperUtils.getModelMapper().map(orderLine, OrderLineResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderLineResponseDTO> getOrderLinesByOrderId(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found with ID: " + orderId));

        List<OrderLine> orderLines = orderLineRepository.findAllByOrder(order);

        return orderLines.stream()
                .map(orderLine -> modelMapperUtils.getModelMapper().map(orderLine, OrderLineResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderLineResponseDTO> getAllOrderLines() {
        List<OrderLine> orderLines = orderLineRepository.findAll();
        return orderLines.stream()
                .map(orderLine -> modelMapperUtils.getModelMapper().map(orderLine, OrderLineResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteOrderLine(Long id) {
        if (!orderLineRepository.existsById(id)) {
            throw new NotFoundException("OrderLine not found with ID: " + id);
        }
        orderLineRepository.deleteById(id);
    }

    @Override
    public OrderLineResponseDTO updateOrderLine(Long id, OrderLineRequestDTO orderLineRequestDTO) {
        OrderLine orderLine = orderLineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("OrderLine not found with ID: " + id));

        modelMapperUtils.getModelMapper().map(orderLineRequestDTO, orderLine);
        orderLine = orderLineRepository.save(orderLine);

        return modelMapperUtils.getModelMapper().map(orderLine, OrderLineResponseDTO.class);
    }

}
