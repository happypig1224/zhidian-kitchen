create table address_book
(
    id            bigint auto_increment comment '主键'
        primary key,
    user_id       bigint                       not null comment '用户id',
    consignee     varchar(50)                  null comment '收货人',
    sex           varchar(2)                   null comment '性别',
    phone         varchar(11)                  not null comment '手机号',
    province_code varchar(12) charset utf8mb4  null comment '省级区划编号',
    province_name varchar(32) charset utf8mb4  null comment '省级名称',
    city_code     varchar(12) charset utf8mb4  null comment '市级区划编号',
    city_name     varchar(32) charset utf8mb4  null comment '市级名称',
    district_code varchar(12) charset utf8mb4  null comment '区级区划编号',
    district_name varchar(32) charset utf8mb4  null comment '区级名称',
    detail        varchar(200) charset utf8mb4 null comment '详细地址',
    label         varchar(100) charset utf8mb4 null comment '标签',
    is_default    tinyint(1) default 0         not null comment '默认 0 否 1是'
)
    comment '地址簿' collate = utf8_bin;

create table category
(
    id          bigint auto_increment comment '主键'
        primary key,
    type        int           null comment '类型   1 菜品分类 2 套餐分类',
    name        varchar(32)   not null comment '分类名称',
    sort        int default 0 not null comment '顺序',
    status      int           null comment '分类状态 0:禁用，1:启用',
    create_time datetime      null comment '创建时间',
    update_time datetime      null comment '更新时间',
    create_user bigint        null comment '创建人',
    update_user bigint        null comment '修改人',
    constraint idx_category_name
        unique (name)
)
    comment '菜品及套餐分类' collate = utf8_bin;

create table chat_memory
(
    id           bigint auto_increment comment '主键ID'
        primary key,
    session_id   varchar(512)                       not null comment '会话ID',
    message_type varchar(20)                        not null comment '消息类型,user-用户消息,assistant-助手消息',
    content      text                               not null comment '消息内容',
    create_time  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_deleted   tinyint  default 0                 not null comment '是否删除，0-否，1-是'
)
    comment '聊天记录表';

create index idx_user_session_id
    on chat_memory (session_id);

create table dish
(
    id           bigint auto_increment comment '主键'
        primary key,
    name         varchar(32)    not null comment '菜品名称',
    category_id  bigint         not null comment '菜品分类id',
    price        decimal(10, 2) null comment '菜品价格',
    image        varchar(255)   null comment '图片',
    description  varchar(255)   null comment '描述信息',
    status       int default 1  null comment '0 停售 1 起售',
    sales_volume int default 0  null comment '销量',
    create_time  datetime       null comment '创建时间',
    update_time  datetime       null comment '更新时间',
    create_user  bigint         null comment '创建人',
    update_user  bigint         null comment '修改人',
    constraint idx_dish_name
        unique (name)
)
    comment '菜品' collate = utf8_bin;

create table dish_flavor
(
    id      bigint auto_increment comment '主键'
        primary key,
    dish_id bigint       not null comment '菜品',
    name    varchar(32)  null comment '口味名称',
    value   varchar(255) null comment '口味数据list'
)
    comment '菜品口味关系表' collate = utf8_bin;

create table employee
(
    id          bigint auto_increment comment '主键'
        primary key,
    name        varchar(32)   not null comment '姓名',
    username    varchar(32)   not null comment '用户名',
    password    varchar(64)   not null comment '密码',
    phone       varchar(11)   not null comment '手机号',
    sex         varchar(2)    not null comment '性别',
    id_number   varchar(18)   not null comment '身份证号',
    status      int default 1 not null comment '状态 0:禁用，1:启用',
    create_time datetime      null comment '创建时间',
    update_time datetime      null comment '更新时间',
    create_user bigint        null comment '创建人',
    update_user bigint        null comment '修改人',
    constraint idx_username
        unique (username)
)
    comment '员工信息' collate = utf8_bin;

create table local_message
(
    id                bigint auto_increment
        primary key,
    business_id       bigint                                not null comment '业务ID',
    message_type      varchar(50)                           not null comment '消息类型',
    content           text                                  null comment '消息内容',
    status            varchar(20) default 'PENDING'         null comment '消息状态',
    retry_count       int         default 0                 null comment '重试次数',
    max_retry_count   int         default 3                 null comment '最大重试次数',
    next_execute_time datetime                              null comment '下次执行时间',
    create_time       datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    update_time       datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    process_time      datetime                              null comment '处理完成时间',
    error_message     text                                  null comment '错误信息'
)
    comment '本地消息表' collate = utf8_bin;

create table order_detail
(
    id          bigint auto_increment comment '主键'
        primary key,
    name        varchar(32)    null comment '名字',
    image       varchar(255)   null comment '图片',
    order_id    bigint         not null comment '订单id',
    dish_id     bigint         null comment '菜品id',
    setmeal_id  bigint         null comment '套餐id',
    dish_flavor varchar(50)    null comment '口味',
    number      int default 1  not null comment '数量',
    amount      decimal(10, 2) not null comment '金额'
)
    comment '订单明细表' collate = utf8_bin;

create table orders
(
    id                      bigint auto_increment comment '主键'
        primary key,
    number                  varchar(50)          null comment '订单号',
    status                  int        default 1 not null comment '订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 7退款',
    user_id                 bigint               not null comment '下单用户',
    address_book_id         bigint               not null comment '地址id',
    order_time              datetime             not null comment '下单时间',
    checkout_time           datetime             null comment '结账时间',
    pay_method              int        default 1 not null comment '支付方式 1微信,2支付宝',
    pay_status              tinyint    default 0 not null comment '支付状态 0未支付 1已支付 2退款',
    amount                  decimal(10, 2)       not null comment '实收金额',
    remark                  varchar(100)         null comment '备注',
    phone                   varchar(11)          null comment '手机号',
    address                 varchar(255)         null comment '地址',
    user_name               varchar(32)          null comment '用户名称',
    consignee               varchar(32)          null comment '收货人',
    cancel_reason           varchar(255)         null comment '订单取消原因',
    rejection_reason        varchar(255)         null comment '订单拒绝原因',
    cancel_time             datetime             null comment '订单取消时间',
    estimated_delivery_time datetime             null comment '预计送达时间',
    delivery_status         tinyint(1) default 1 not null comment '配送状态  1立即送出  0选择具体时间',
    delivery_time           datetime             null comment '送达时间',
    pack_amount             int                  null comment '打包费',
    tableware_number        int                  null comment '餐具数量',
    tableware_status        tinyint(1) default 1 not null comment '餐具数量状态  1按餐量提供  0选择具体数量',
    version                 int        default 0 null comment '版本号'
)
    comment '订单表' collate = utf8_bin;

create table seckill_order
(
    id          bigint auto_increment comment '主键'
        primary key,
    user_id     bigint                        not null comment '用户id',
    voucher_id  bigint                        not null comment '优惠券id',
    order_time  datetime                      not null comment '下单时间',
    status      tinyint(1) unsigned default 1 not null comment '1,待支付; 2,已支付; 3,已取消; 4,已退款; 5,支付失败; 6,已删除',
    create_time datetime                      not null comment '创建时间'
)
    comment '秒杀订单表' collate = utf8_bin;

create table seckill_voucher
(
    id           bigint auto_increment comment '主键'
        primary key comment '主键',
    voucher_id   bigint                        not null comment '优惠券id',
    title        varchar(255)                  not null comment '优惠卷标题',
    rules        varchar(1024)                 null comment '使用规则',
    pay_value    bigint(10) unsigned           not null comment '支付金额，单位是分。例如200代表2元',
    actual_value bigint(10)                    not null comment '抵扣金额，单位是分。例如200代表2元',
    status       tinyint(1) unsigned default 1 not null comment '1,上架; 2,下架; 3,过期',
    stock        int                           not null comment '库存',
    begin_time   datetime                      not null comment '生效时间',
    end_time     datetime                      not null comment '失效时间',
    create_time  datetime                      not null comment '创建时间'
)
    comment '秒杀优惠券表' collate = utf8_bin;

create table setmeal
(
    id           bigint auto_increment comment '主键'
        primary key,
    category_id  bigint         not null comment '菜品分类id',
    name         varchar(32)    not null comment '套餐名称',
    price        decimal(10, 2) not null comment '套餐价格',
    status       int default 1  null comment '售卖状态 0:停售 1:起售',
    description  varchar(255)   null comment '描述信息',
    image        varchar(255)   null comment '图片',
    sales_volume int default 0  null comment '销量',
    create_time  datetime       null comment '创建时间',
    update_time  datetime       null comment '更新时间',
    create_user  bigint         null comment '创建人',
    update_user  bigint         null comment '修改人',
    constraint idx_setmeal_name
        unique (name)
)
    comment '套餐' collate = utf8_bin;

create table setmeal_dish
(
    id         bigint auto_increment comment '主键'
        primary key,
    setmeal_id bigint         null comment '套餐id',
    dish_id    bigint         null comment '菜品id',
    name       varchar(32)    null comment '菜品名称 （冗余字段）',
    price      decimal(10, 2) null comment '菜品单价（冗余字段）',
    copies     int            null comment '菜品份数'
)
    comment '套餐菜品关系' collate = utf8_bin;

create table shopping_cart
(
    id          bigint auto_increment comment '主键'
        primary key,
    name        varchar(32)    null comment '商品名称',
    image       varchar(255)   null comment '图片',
    user_id     bigint         not null comment '主键',
    dish_id     bigint         null comment '菜品id',
    setmeal_id  bigint         null comment '套餐id',
    dish_flavor varchar(50)    null comment '口味',
    number      int default 1  not null comment '数量',
    amount      decimal(10, 2) not null comment '金额',
    create_time datetime       null comment '创建时间'
)
    comment '购物车' collate = utf8_bin;

create table user
(
    id          bigint auto_increment comment '主键'
        primary key,
    openid      varchar(45)  null comment '微信用户唯一标识',
    name        varchar(32)  null comment '姓名',
    phone       varchar(11)  null comment '手机号',
    sex         varchar(2)   null comment '性别',
    id_number   varchar(18)  null comment '身份证号',
    avatar      varchar(500) null comment '头像',
    create_time datetime     null
)
    comment '用户信息' collate = utf8_bin;


