```bash
# forgot password local URL
curl -X POST "http://localhost:8000/api/user/forgot/password?email=ken@lektralabs.com"

# use the password you get in the email below to see if it works 
curl -X POST --header "Content-Type: application/json" --data '{"username" : "kbrumer", "password" : "pvEVmegc"}' http://localhost:8000/api/sso/login

# forgot username
curl -X POST "http://localhost:8000/api/user/forgot/username?email=ken@lektralabs.com"
```