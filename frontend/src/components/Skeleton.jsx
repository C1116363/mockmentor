/**
 * Placeholder shapes while data loads.
 *
 * "Loading..." tells you nothing except that you are waiting. A skeleton shows
 * the shape of what is coming, so the layout does not jump when it arrives and
 * the wait feels shorter than it is - the page looks like it is already there.
 *
 * @param rows  how many placeholder cards
 * @param lines lines of text inside each one
 */
export default function Skeleton({ rows = 3, lines = 3 }) {
  return (
    <div className="card-list" aria-busy="true" aria-label="Loading">
      {Array.from({ length: rows }, (_, i) => (
        <div className="skel-card" key={i}>
          <div className="skel skel--title" />
          {Array.from({ length: lines }, (_, j) => (
            <div
              className="skel"
              key={j}
              // Ragged line lengths, because a stack of identical bars reads as a
              // loading graphic rather than as text about to appear.
              style={{ width: `${[92, 74, 58, 81][(i + j) % 4]}%` }}
            />
          ))}
        </div>
      ))}
    </div>
  );
}
