# Dockerfile para el servidor de desarrollo Vite
# Nota: Electron se ejecuta en el host — este contenedor expone solo el servidor web React.
FROM node:20-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --prefer-offline

COPY . .

EXPOSE 5173

CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
