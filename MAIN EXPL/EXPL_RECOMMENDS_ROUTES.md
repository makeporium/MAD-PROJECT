# Comprehensive File Analysis: `backend/src/routes/recommendsRoutes.js`

This file is a hybrid route. It bridges the traditional Relational Database with modern Generative AI by injecting structured SQL data into an unstructured prompt.

## 1. POST `/ai-search` (Retrieval-Augmented Generation)

### The SQL Queries
```javascript
    const [articles] = await sequelize.query("SELECT id, title, topic, excerpt, tags FROM info_resources LIMIT 50");
    const [events] = await sequelize.query(`
      SELECT e.id, e.title, e.description, e.event_date, u.name as expert_name 
      FROM events e LEFT JOIN users u ON e.expert_id = u.id 
      WHERE e.event_date >= NOW() LIMIT 20
    `);
    const [experts] = await sequelize.query(`
      SELECT ep.user_id, u.name, ep.specialty, ep.location 
      FROM expert_profiles ep JOIN users u ON ep.user_id = u.id LIMIT 20
    `);
```

### Deep Dive: Concepts and Theory
- **Data Query Language (DQL)**: This block executes three separate `SELECT` queries back-to-back.
- **RAG (Retrieval-Augmented Generation)**: The core concept here isn't a complex SQL feature, but how we *use* the SQL output. LLMs (like Gemini) have a knowledge cutoff and know nothing about our private database. By running these queries and serializing the results into JSON strings (`JSON.stringify(articles)`), we physically embed our database's current state into the LLM's prompt. 
- **Filtering (`WHERE e.event_date >= NOW()`)**: We intentionally use a scalar date function `NOW()` so that the AI is never fed events that have already passed. This saves Tokens (lowering API costs) and prevents the AI from hallucinating a recommendation for an expired event.
- **Table Joins for AI Context**:
  - `expert_profiles ep JOIN users u`: The `expert_profiles` table doesn't have the user's name (to maintain 3NF). But the AI needs to know the expert's name to recommend them properly. We use an `INNER JOIN` to fetch `u.name` and feed that unified data object to the AI.
