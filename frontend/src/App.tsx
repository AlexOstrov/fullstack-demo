import { useCallback, useEffect, useState, type FormEvent } from "react";
import axios from "axios";

type Item = {
  id: number;
  title: string;
  createdAt: string;
};

export default function App() {
  const [items, setItems] = useState<Item[]>([]);
  const [title, setTitle] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const loadItems = useCallback(async () => {
    try {
      const response = await axios.get<Item[]>("/api/items");
      setItems(response.data);
      setError("");
    } catch {
      setError("Не удалось загрузить список элементов");
    }
  }, []);

  useEffect(() => {
    loadItems();
  }, [loadItems]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();

    if (!title.trim()) {
      return;
    }

    setLoading(true);

    try {
      await axios.post("/api/items", { title: title.trim() });
      setTitle("");
      await loadItems();
    } catch {
      setError("Не удалось добавить элемент");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 720, margin: "40px auto", fontFamily: "Arial" }}>
      <h1>Spring Boot + React + PostgreSQL</h1>

      <form onSubmit={handleSubmit} style={{ marginBottom: 24 }}>
        <input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="Введите название"
          style={{
            padding: 8,
            width: 320,
            marginRight: 8,
          }}
        />
        <button
          type="submit"
          disabled={loading}
          style={{
            padding: "8px 16px",
          }}
        >
          {loading ? "Сохранение..." : "Добавить"}
        </button>
      </form>

      {error && (
        <div style={{ color: "red", marginBottom: 16 }}>
          {error}
        </div>
      )}

      <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
          <tr>
            <th style={{ border: "1px solid #ddd", padding: 8 }}>ID</th>
            <th style={{ border: "1px solid #ddd", padding: 8 }}>Title</th>
            <th style={{ border: "1px solid #ddd", padding: 8 }}>Created At</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.id}>
              <td style={{ border: "1px solid #ddd", padding: 8 }}>{item.id}</td>
              <td style={{ border: "1px solid #ddd", padding: 8 }}>{item.title}</td>
              <td style={{ border: "1px solid #ddd", padding: 8 }}>
                {new Date(item.createdAt).toLocaleString()}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}