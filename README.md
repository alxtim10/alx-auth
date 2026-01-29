# Auth Service (Quarkus + Java 21)

Backend authentication & user management service built with **Quarkus**, **Java 21**, **PostgreSQL**, and **JWT**.

This service provides:
- Authentication (register, login, refresh token, logout)
- User management (admin-only CRUD + pagination + sorting + filtering)
- Password management (change password, forgot/reset password)
- Email verification flow
- Soft delete user
- Audit logging
- Standard JSON error response format

---

## Tech Stack
- **Java 21 (LTS)**
- **Quarkus 3**
- **PostgreSQL**
- **Hibernate ORM Panache**
- **JWT (SmallRye JWT)**
- **BCrypt** for password hashing

---

## Project Features

### ✅ Authentication
- Register user
- Login user (JWT access token + refresh token)
- Refresh access token (**refresh token rotation**)
- Logout (revokes refresh token)

### ✅ User Management (Admin)
- List users with:
  - Pagination (`page`, `size`)
  - Sorting (`sort`, `dir`)
  - Filtering (`q`, `role`, `active`, `createdFrom`, `createdTo`)
- Update user (admin)
- Soft delete user (admin)

### ✅ Password & Security
- Change password (authenticated user)
- Forgot password (request reset token)
- Reset password (using reset token)

### ✅ Verification
- Request email verification
- Verify email using token

### ✅ Audit
- Audit log table available for important admin/user actions
- Soft delete revokes user refresh sessions

### ✅ Standard Error Response
All API errors return a consistent JSON format:
```json
{
  "code": "BAD_REQUEST",
  "message": "email is required",
  "traceId": "uuid-here",
  "details": {
    "status": 400
  }
}

### Run PostgreSQL with Docker

Example:

docker run -d \
  --name default_db \
  -e POSTGRES_DB=default_be \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5433:5432 \
  postgres:16

✅ Access DB:
docker exec -it default_db psql -U postgres -d default_be

### JWT Keys Setup

Generate keys:

openssl genrsa -out src/main/resources/privateKey.pem 2048
openssl rsa -in src/main/resources/privateKey.pem -pubout \
  -out src/main/resources/publicKey.pem

### Postman Collection

```json
{
  "info": {
    "_postman_id": "5e61d3b9-1d1a-4d5b-9b21-93b2ddf9c2a7",
    "name": "Auth Service - Quarkus (JWT + Refresh + Users)",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    "description": "Auth + User Management API (register/login/refresh/logout, email verify, forgot/reset, change password, admin users list/update/delete)."
  },
  "item": [
    {
      "name": "00 - Health Check (optional)",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "{{baseUrl}}/q/health",
          "host": [
            "{{baseUrl}}"
          ],
          "path": [
            "q",
            "health"
          ]
        }
      }
    },
    {
      "name": "Auth",
      "item": [
        {
          "name": "01 - Register",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"{{username}}\",\n  \"email\": \"{{email}}\",\n  \"password\": \"{{password}}\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/auth/register",
              "host": [
                "{{baseUrl}}"
              ],
              "path": [
                "auth",
                "register"
              ]
            }
          }
        },
        {
          "name": "02 - Login (stores access_token + refresh_token)",
          "event": [
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status is 200', function () {",
                  "  pm.expect(pm.response.code).to.be.oneOf([200, 201]);",
                  "});",
                  "",
                  "let json;",
                  "try { json = pm.response.json(); } catch (e) { json = null; }",
                  "if (json && json.access_token) {",
                  "  pm.collectionVariables.set('access_token', json.access_token);",
                  "}",
                  "if (json && json.refresh_token) {",
                  "  pm.collectionVariables.set('refresh_token', json.refresh_token);",
                  "}"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"{{username}}\",\n  \"password\": \"{{password}}\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/auth/login",
              "host": [
                "{{baseUrl}}"
              ],
              "path": [
                "auth",
                "login"
              ]
            }
          }
        },
        {
          "name": "03 - Refresh (rotates, stores new tokens)",
          "event": [
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status is 200', function () {",
                  "  pm.expect(pm.response.code).to.equal(200);",
                  "});",
                  "",
                  "let json;",
                  "try { json = pm.response.json(); } catch (e) { json = null; }",
                  "if (json && json.access_token) {",
                  "  pm.collectionVariables.set('access_token', json.access_token);",
                  "}",
                  "if (json && json.refresh_token) {",
                  "  pm.collectionVariables.set('refresh_token', json.refresh_token);",
                  "}"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"refresh_token\": \"{{refresh_token}}\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/auth/refresh",
              "host": [
                "{{baseUrl}}"
              ],
              "path": [
                "auth",
                "refresh"
              ]
            }
          }
        },
        {
          "name": "04 - Logout (revokes refresh_token)",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"refresh_token\": \"{{refresh_token}}\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/auth/logout",
              "host": [
                "{{baseUrl}}"
              ],
              "path": [
                "auth",
                "logout"
              ]
            }
          }
        },
        {
          "name": "05 - Change Password (USER/ADMIN)",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{access_token}}"
              },
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"old_password\": \"{{password}}\",\n  \"new_password\": \"{{new_password}}\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/auth/change-password",
              "host": [
                "{{baseUrl}}"
              ],
              "path": [
                "auth",
                "change-password"
              ]
            }
          }
        }
      ]
    },
    {
      "name": "Verification & Password Reset",
      "item": [
        {
          "name": "06 - Request Verify Email (stores verify_token if returned)",
          "event": [
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status is 200', function () {",
                  "  pm.expect(pm.response.code).to.equal(200);",
                  "});",
                  "",
                  "let json;",
                  "try { json = pm.response.json(); } catch (e) { json = null; }",
                  "if (json && json.verify_token) {",
                  "  pm.collectionVariables.set('verify_token', json.verify_token);",
                  "}"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"email\": \"{{email}}\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/auth/request-verify",
              "host": [
                "{{baseUrl}}"
              ],
              "path": [
                "auth",
                "request-verify"
              ]
            }
          }
        },
        {
          "name": "07 - Verify Email",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"token\": \"{{verify_token}}\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/auth/verify-email",
              "host": [
                "{{baseUrl}}"
              ],
              "path": [
                "auth",
                "verify-email"
              ]
            }
          }
        },
        {
          "name": "08 - Forgot Password (stores reset_token if returned)",
          "event": [
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status is 200', function () {",
                  "  pm.expect(pm.response.code).to.equal(200);",
                  "});",
                  "",
                  "let json;",
                  "try { json = pm.response.json(); } catch (e) { json = null; }",
                  "if (json && json.reset_token) {",
                  "  pm.collectionVariables.set('reset_token', json.reset_token);",
                  "}"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"email\": \"{{email}}\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/auth/forgot-password",
              "host": [
                "{{baseUrl}}"
              ],
              "path": [
                "auth",
                "forgot-password"
              ]
            }
          }
        },
        {
          "name": "09 - Reset Password",
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"token\": \"{{reset_token}}\",\n  \"new_password\": \"{{new_password}}\"\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/auth/reset-password",
              "host": [
                "{{baseUrl}}"
              ],
              "path": [
                "auth",
                "reset-password"
              ]
            }
          }
        }
      ]
    },
    {
      "name": "Users (Admin Only)",
      "item": [
        {
          "name": "10 - List Users (paged + sort + filter)",
          "request": {
            "method": "GET",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{access_token}}"
              }
            ],
            "url": {
              "raw": "{{baseUrl}}/users?page={{page}}&size={{size}}&sort={{sort}}&dir={{dir}}&q={{q}}&role={{role}}&active={{active}}",
              "host": [
                "{{baseUrl}}"
              ],
              "path": [
                "users"
              ],
              "query": [
                {
                  "key": "page",
                  "value": "{{page}}"
                },
                {
                  "key": "size",
                  "value": "{{size}}"
                },
                {
                  "key": "sort",
                  "value": "{{sort}}"
                },
                {
                  "key": "dir",
                  "value": "{{dir}}"
                },
                {
                  "key": "q",
                  "value": "{{q}}"
                },
                {
                  "key": "role",
                  "value": "{{role}}"
                },
                {
                  "key": "active",
                  "value": "{{active}}"
                }
              ]
            }
          }
        },
        {
          "name": "11 - Update User (Admin)",
          "request": {
            "method": "PUT",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{access_token}}"
              },
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"email\": \"{{update_email}}\",\n  \"active\": {{update_active}}\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/users/{{user_id}}",
              "host": [
                "{{baseUrl}}"
              ],
              "path": [
                "users",
                "{{user_id}}"
              ]
            }
          }
        },
        {
          "name": "12 - Delete User (Soft Delete) (Admin)",
          "request": {
            "method": "DELETE",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{access_token}}"
              }
            ],
            "url": {
              "raw": "{{baseUrl}}/users/{{user_id}}",
              "host": [
                "{{baseUrl}}"
              ],
              "path": [
                "users",
                "{{user_id}}"
              ]
            }
          }
        }
      ]
    }
  ],
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080"
    },
    {
      "key": "username",
      "value": "alex"
    },
    {
      "key": "email",
      "value": "alex@mail.com"
    },
    {
      "key": "password",
      "value": "secret123"
    },
    {
      "key": "new_password",
      "value": "newsecret123"
    },
    {
      "key": "access_token",
      "value": ""
    },
    {
      "key": "refresh_token",
      "value": ""
    },
    {
      "key": "verify_token",
      "value": ""
    },
    {
      "key": "reset_token",
      "value": ""
    },
    {
      "key": "page",
      "value": "1"
    },
    {
      "key": "size",
      "value": "10"
    },
    {
      "key": "sort",
      "value": "id"
    },
    {
      "key": "dir",
      "value": "desc"
    },
    {
      "key": "q",
      "value": ""
    },
    {
      "key": "role",
      "value": ""
    },
    {
      "key": "active",
      "value": ""
    },
    {
      "key": "user_id",
      "value": "1"
    },
    {
      "key": "update_email",
      "value": "alex.updated@mail.com"
    },
    {
      "key": "update_active",
      "value": "true"
    }
  ]
}
