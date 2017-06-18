package net.jotorren.microservices.rtsba.participant;

public enum RtsBaPropagation {

    /**
     *  <p>If called outside a transaction context, the interceptor must begin a new
     *  Composite transaction, the managed bean method execution must then continue
     *  inside this transaction context, and the transaction must be completed by
     *  the interceptor.</p>
     *  <p>If called inside a transaction context, the managed bean
     *  method execution must then continue inside this transaction context.</p>
     */
    REQUIRED,

    /**
     *  <p>If called outside a transaction context, the interceptor must begin a new
     *  Composite transaction, the managed bean method execution must then continue
     *  inside this transaction context, and the transaction must be completed by
     *  the interceptor.</p>
     *  <p>If called inside a transaction context, the current transaction context must
     *  be suspended, a new Composite transaction will begin, the managed bean method
     *  execution must then continue inside this transaction context, the transaction
     *  must be completed, and the previously suspended transaction must be resumed.</p>
     */
    REQUIRES_NEW,

    /**
     *  <p>If called outside a transaction context, a TransactionalException with a
     *  nested TransactionRequiredException must be thrown.</p>
     *  <p>If called inside a transaction context, managed bean method execution will
     *  then continue under that context.</p>
     */
    MANDATORY,

    /**
     *  <p>If called outside a transaction context, managed bean method execution
     *  must then continue outside a transaction context.</p>
     *  <p>If called inside a transaction context, the managed bean method execution
     *  must then continue inside this transaction context.</p>
     */
    SUPPORTS,

    /**
     *  <p>If called outside a transaction context, managed bean method execution
     *  must then continue outside a transaction context.</p>
     *  <p>If called inside a transaction context, the current transaction context must
     *  be suspended, the managed bean method execution must then continue
     *  outside a transaction context, and the previously suspended transaction
     *  must be resumed by the interceptor that suspended it after the method
     *  execution has completed.</p>
     */
    NOT_SUPPORTED,

    /**
     *  <p>If called outside a transaction context, managed bean method execution
     *  must then continue outside a transaction context.</p>
     *  <p>If called inside a transaction context, a TransactionalException with
     *  a nested InvalidTransactionException must be thrown.</p>
     */
    NEVER
}
