# BankSphere Microservices - Quick Start Guide

## 🚀 Quick Start - 5 Minutes

### Step 1: Open 6 Terminal Windows

### Step 2: Start Services in Order

**Terminal 1: Eureka Server**
```bash
cd C:\Users\2484948\Desktop\BANKAPP\BankSphere\eureka-server
mvn clean spring-boot:run
# Wait for: "Eureka Server started in X ms"
```

**Terminal 2: Customer Service**
```bash
cd C:\Users\2484948\Desktop\BANKAPP\BankSphere\customer-services
mvn clean spring-boot:run
# Wait for: "Register with Eureka: registering application CUSTOMER-SERVICE"
```

**Terminal 3: Account Service**
```bash
cd C:\Users\2484948\Desktop\BANKAPP\BankSphere\Account-service
mvn clean spring-boot:run
# Wait for: "Register with Eureka: registering application ACCOUNT-SERVICE"
```

**Terminal 4: Transaction Service**
```bash
cd C:\Users\2484948\Desktop\BANKAPP\BankSphere\Transaction-service
mvn clean spring-boot:run
# Wait for: "Register with Eureka: registering application TRANSACTION-SERVICE"
```

**Terminal 5: Loan Service**
```bash
cd C:\Users\2484948\Desktop\BANKAPP\BankSphere\loan-service
mvn clean spring-boot:run
# Wait for: "Register with Eureka: registering application LOAN-SERVICE"
```

**Terminal 6: Identity Service**
```bash
cd C:\Users\2484948\Desktop\BANKAPP\BankSphere\identity-services
mvn clean spring-boot:run
# Wait for: "Register with Eureka: registering application IDENTITY-SERVICE"
```

### Step 3: Start API Gateway (wait 30 seconds after all services start)

**Terminal 7: API Gateway**
```bash
cd C:\Users\2484948\Desktop\BANKAPP\BankSphere\api-gateway
mvn clean spring-boot:run
# Wait for: "Grpc started on port(s)"
```

---

## 📊 Verify Everything is Running

### ✅ Check Eureka Dashboard
Open browser: **http://localhost:8761**

You should see:
- **Instances currently registered with Eureka**
  - ACCOUNT-SERVICE (8083)
  - CUSTOMER-SERVICE (8081)
  - TRANSACTION-SERVICE (8082)
  - LOAN-SERVICE (8085)
  - IDENTITY-SERVICE (8084)

### ✅ Check Gateway Routing
```bash
# Test customer service through gateway
curl http://localhost:8080/customers

# Should return success or validation error (service is working)
```

### ✅ Check Service Health
```bash
# Core Gateway
curl http://localhost:8080/actuator/health

# Each service individually
curl http://localhost:8081/actuator/health    # Customer
curl http://localhost:8083/actuator/health    # Account
curl http://localhost:8082/actuator/health    # Transaction
curl http://localhost:8085/actuator/health    # Loan
curl http://localhost:8084/actuator/health    # Identity
```

---

## 🧪 Test Scenarios

### Test 1: Service Discovery & Gateway Routing
```bash
# Make a request through API Gateway (port 8080)
curl http://localhost:8080/customers

# Gateway should route to customer-service (8081) automatically
# ✅ This proves: Service Discovery + Gateway Routing works
```

### Test 2: Check Circuit Breaker Status
```bash
# Check health with circuit breaker details
curl http://localhost:8080/actuator/health
```

Expected output shows circuit breaker for each service:
```json
{
  "circuitbreakers": {
    "customerServiceCB": "UP",
    "accountServiceCB": "UP",
    "transactionServiceCB": "UP",
    "loanServiceCB": "UP",
    "identityServiceCB": "UP"
  }
}
```

### Test 3: Feign Client Communication
```bash
# Loan service calls customer service via Feign
# (Loan Service will fetch customer details from Customer Service internally)
curl -X POST http://localhost:8080/loans/eligibility \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST001"}'

# ✅ This proves: Feign client + Circuit Breaker + Resilience4j works
```

### Test 4: Test Fallback Behavior (Simulate Service Down)

**Step 1**: Stop account-service (Ctrl+C in Terminal 3)

**Step 2**: Make a request that requires account service:
```bash
curl -X POST http://localhost:8080/loans/eligibility \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST001"}'

# ✅ You should see: Circuit breaker opens → Fallback method is called
# ✅ This proves: Fault tolerance works
```

**Step 3**: Check circuit breaker status:
```bash
curl http://localhost:8080/actuator/health
# You'll see: "accountServiceCB": "CIRCUIT_BROKEN"
```

**Step 4**: Restart account-service (command in Terminal 3)
```bash
mvn clean spring-boot:run
```

**Step 5**: Circuit breaker transitions to HALF_OPEN, then back to UP

---

## 📱 API Endpoints Through Gateway

### Customer Service
```
GET    http://localhost:8080/customers
POST   http://localhost:8080/customers
GET    http://localhost:8080/customers/{id}
PUT    http://localhost:8080/customers/{id}
DELETE http://localhost:8080/customers/{id}
```

### Account Service
```
GET    http://localhost:8080/accounts/me
GET    http://localhost:8080/accounts/{accountNo}
POST   http://localhost:8080/accounts
```

### Transaction Service
```
POST   http://localhost:8080/transactions
GET    http://localhost:8080/transactions/{id}
GET    http://localhost:8080/transactions/account/{accountId}
```

### Loan Service
```
GET    http://localhost:8080/loans
POST   http://localhost:8080/loans
POST   http://localhost:8080/loans/eligibility
```

### Identity Service
```
POST   http://localhost:8080/auth/signup
POST   http://localhost:8080/auth/login
```

---

## 🔄 Request Flow Example

**User makes a Loan Application request:**

```
1. Browser/Client
   ↓
2. API Gateway (8080) receives: POST /loans/eligibility
   ↓
3. Gateway looks up "loan-service" in Eureka registry
   ↓
4. Gateway routes to Loan Service (8085)
   ↓
5. Loan Service needs to verify customer eligibility
   | └─→ Loan Service uses Feign Client to call:
   |     POST /api/v1/customers/internal/check-eligibility
   |    └─→ This goes through load balancer to Customer Service (8081)
   |       └─→ With Circuit Breaker protection
   |
6. Loan Service gets response and continues processing
   ↓
7. Response sent back through Gateway to Client
```

**If Customer Service is Down:**
```
5.1. Loan Service Feign call to Customer Service
     ↓
5.2. Request fails (service down)
     ↓
5.3. Circuit Breaker catches the failure
     ↓
5.4. After N failed requests, Circuit Breaker OPENS
     ↓
5.5. Fallback method is called immediately
     ↓
5.6. User gets graceful error message
     └─→ No cascading failure! Other services keep working
```

---

## 🛠️ Monitoring & Debugging

### View Eureka Discovery
```
http://localhost:8761
```
Shows:
- All registered services ✅
- Service health status ✅
- Instance ID and ports ✅

### View Circuit Breaker Status
```bash
curl http://localhost:8080/actuator/circuitbreakers
```

### View Service Logs (in Docker/Console)
```
Look for keywords:
- "Eureka" → Service registration messages
- "CircuitBreaker" → Circuit breaker state changes
- "Feign" → Inter-service calls
- "RetryableException" → Retry attempts
```

---

## 🔧 Troubleshooting Commands

### ✅ Check if port is running
```powershell
netstat -ano | findstr ":8761"    # Eureka
netstat -ano | findstr ":8080"    # Gateway
netstat -ano | findstr ":8081"    # Customer
netstat -ano | findstr ":8083"    # Account
netstat -ano | findstr ":8082"    # Transaction
netstat -ano | findstr ":8085"    # Loan
netstat -ano | findstr ":8084"    # Identity
```

### ✅ Kill and restart a service
```powershell
# Find process ID
netstat -ano | findstr ":8085"

# Kill process (replace PID)
taskkill /PID <PID> /F

# Restart
mvn clean spring-boot:run
```

### ✅ Check database connections
```bash
# If you see "Connection refused"
# Make sure MySQL is running:
# Windows: Check Services (services.msc)
# Or restart MySQL: net stop MySQL80 && net start MySQL80
```

### ✅ View Eureka Logs
Look in console for:
```
InstanceInfoReplicator: onDemandUpdate
ERR - statusUpdate() failed for app
Successfully registered application
```

---

## 🎯 What to Test

1. ✅ All services show in Eureka dashboard
2. ✅ Requests route through API Gateway (port 8080)
3. ✅ Swagger/OpenAPI available:
   - http://localhost:8081/swagger-ui.html (Customer)
   - http://localhost:8083/swagger-ui.html (Account)
   - http://localhost:8082/swagger-ui.html (Transaction)
   - http://localhost:8085/swagger-ui.html (Loan)
4. ✅ Circuit breakers are UP when all services running
5. ✅ Circuit breaker goes OPEN when service is down
6. ✅ Service recovers and goes UP after restart
7. ✅ Feign calls work (inter-service communication)
8. ✅ Fallback methods work when service is unavailable

---

## 📞 Port Reference

| Service | Direct | Via Gateway | Status |
|---------|--------|-------------|--------|
| Eureka | 8761 | N/A | Discovery |
| Gateway | N/A | 8080 | Router |
| Customer | 8081 | 8080/customers | ✅ |
| Transaction | 8082 | 8080/transactions | ✅ |
| Account | 8083 | 8080/accounts | ✅ |
| Identity | 8084 | 8080/auth | ✅ |
| Loan | 8085 | 8080/loans | ✅ |

---

## ✅ Ready to Go!

Everything is now configured for a production-ready microservices architecture with:
- ✅ Service Discovery (Eureka)
- ✅ API Gateway Routing (Spring Cloud Gateway)
- ✅ Inter-Service Communication (OpenFeign)
- ✅ Fault Tolerance (Resilience4j)
- ✅ Load Balancing (Spring Cloud LoadBalancer)
- ✅ Circuit Breaker Pattern
- ✅ Retry Logic
- ✅ Timeout Protection
- ✅ Health Checks
- ✅ Graceful Degradation

**Start services and enjoy! 🎉**

