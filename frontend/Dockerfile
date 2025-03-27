# Node.js 18 kullan
FROM node:18-alpine

WORKDIR /app

COPY package.json package-lock.json ./

RUN npm install

# Bağımlılıkları yükle
COPY . .

# Next.js başlat
CMD ["npm", "run", "dev"]

# Dosya değişikliklerini dinlemesi için watchman veya inotify limitlerini artır
RUN echo fs.inotify.max_user_watches=524288 | tee -a /etc/sysctl.conf && sysctl -p