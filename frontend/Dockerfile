# Node.js 18 kullan
FROM node:18-alpine
WORKDIR /app

# Bağımlılıkları yükle
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Next.js başlat
CMD ["npm", "run", "start"]
